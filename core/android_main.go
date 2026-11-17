//go:build android

// Android entry point: run as a child process of the app's VpnService.
//   libxkcore.so <config-file> <asset-dir> <abstract-socket-name>
// The TUN fd is received over the abstract unix socket (SCM_RIGHTS).
package main

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net"
	"os"
	"os/signal"
	"runtime/debug"
	"syscall"

	"github.com/xtls/xray-core/core"
	_ "github.com/xtls/xray-core/main/distro/all"
)

type androidCfg struct {
	Sub   string `json:"sub"`
	Index int    `json:"index"`
	Opts  string `json:"opts"`
}

func recvFd(name string) (int, error) {
	c, err := net.DialUnix("unix", nil, &net.UnixAddr{Name: "@" + name, Net: "unix"})
	if err != nil {
		return -1, err
	}
	defer c.Close()
	buf := make([]byte, 8)
	oob := make([]byte, syscall.CmsgSpace(4*4))
	_, oobn, _, _, err := c.ReadMsgUnix(buf, oob)
	if err != nil {
		return -1, err
	}
	msgs, err := syscall.ParseSocketControlMessage(oob[:oobn])
	if err != nil || len(msgs) == 0 {
		return -1, fmt.Errorf("no control message")
	}
	fds, err := syscall.ParseUnixRights(&msgs[0])
	if err != nil || len(fds) == 0 {
		return -1, fmt.Errorf("no fd")
	}
	return fds[0], nil
}

func main() {
	if len(os.Args) < 4 && !(len(os.Args) >= 2 && os.Args[1] == "measure") {
		fail("usage")
	}
	debug.SetGCPercent(20)
	// Android has no /etc/resolv.conf: resolve our own lookups via Yandex DNS.
	// The app UID is excluded from the VPN, so this goes out directly.
	net.DefaultResolver = &net.Resolver{PreferGo: true, Dial: func(ctx context.Context, network, address string) (net.Conn, error) {
		d := net.Dialer{}
		c, err := d.DialContext(ctx, "udp", "77.88.8.8:53")
		if err != nil {
			c, err = d.DialContext(ctx, "udp", "1.1.1.1:53")
		}
		return c, err
	}}

	if os.Args[1] == "measure" { // measure <sub-file> <url> <timeout-ms> <i,j,k>
		dbg = os.Getenv("XKDBG") != ""
		runMeasure()
		return
	}
	raw, err := os.ReadFile(os.Args[1])
	if err != nil {
		fail("config: " + err.Error())
	}
	var ac androidCfg
	if err := json.Unmarshal(raw, &ac); err != nil {
		fail("config json: " + err.Error())
	}
	os.Setenv("xray.location.asset", os.Args[2])

	obj, _, err := pickConfig(selectIndex([]byte(ac.Sub), ac.Index))
	if err != nil {
		fail(err.Error())
	}
	addRouting(obj, parseOpts(ac.Opts))
	sanitizeGeo(obj, os.Args[2])
	cfg, err := buildTunConfig(obj)
	if err != nil {
		fail(err.Error())
	}

	fd, err := recvFd(os.Args[3])
	if err != nil {
		fail("tun fd: " + err.Error())
	}
	os.Setenv("xray.tun.fd", fmt.Sprint(fd))

	cfg = withStats(cfg)
	config, err := core.LoadConfig("json", bytes.NewReader(cfg))
	if err != nil {
		fail("ошибка конфига: " + err.Error())
	}
	inst, err := core.New(config)
	if err != nil {
		fail("ошибка запуска: " + err.Error())
	}
	if err := inst.Start(); err != nil {
		fail("ошибка старта: " + err.Error())
	}
	fmt.Println("XKOK")
	go statsLoop(inst)
	debug.FreeOSMemory()

	sig := make(chan os.Signal, 1)
	signal.Notify(sig, syscall.SIGTERM, syscall.SIGINT)
	<-sig
	inst.Close()
}
