package main

/*
#include <stdlib.h>
*/
import "C"

import (
	"bytes"
	"context"
	"io"
	"time"

	xnet "github.com/xtls/xray-core/common/net"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"os"
	"runtime/debug"
	"sync"
	"unsafe"

	"github.com/xtls/xray-core/core"
	_ "github.com/xtls/xray-core/main/distro/all"
)

var (
	mu       sync.Mutex
	instance *core.Instance
)

func cstr(s string) *C.char { return C.CString(s) }

//export XKFree
func XKFree(p *C.char) { C.free(unsafe.Pointer(p)) }

//export XKVersion
func XKVersion() *C.char { return cstr(core.Version()) }


// XKServers returns a JSON array of {"index":i,"remarks":"..."} for all
// configs in the subscription body.
//
//export XKServers
func XKServers(body *C.char) *C.char {
	raw := bytes.TrimSpace([]byte(C.GoString(body)))
	if len(raw) > 0 && raw[0] != '{' && raw[0] != '[' {
		if dec, err := base64.StdEncoding.DecodeString(string(raw)); err == nil {
			raw = bytes.TrimSpace(dec)
		}
	}
	type item struct {
		Index   int    `json:"index"`
		Remarks string `json:"remarks"`
	}
	out := []item{}
	if len(raw) > 0 && raw[0] == '[' {
		var arr []map[string]any
		if json.Unmarshal(raw, &arr) == nil {
			for i, o := range arr {
				r, _ := o["remarks"].(string)
				out = append(out, item{i, r})
			}
		}
	} else if len(raw) > 0 && raw[0] == '{' {
		var o map[string]any
		if json.Unmarshal(raw, &o) == nil {
			r, _ := o["remarks"].(string)
			out = append(out, item{0, r})
		}
	}
	b, _ := json.Marshal(out)
	return cstr(string(b))
}



// XKStart starts Xray. body: raw subscription content, index: server index,
// tunFd: utun fd from NetworkExtension, assetDir: dir with geoip/geosite.
// Returns "" on success or an error message.
//
//export XKStart
func XKStart(body *C.char, index C.int, tunFd C.int, assetDir *C.char, opts *C.char) *C.char {
	mu.Lock()
	defer mu.Unlock()
	debug.SetGCPercent(10)
	debug.SetMemoryLimit(30 << 20)

	if instance != nil {
		_ = instance.Close()
		instance = nil
	}
	os.Setenv("xray.location.asset", C.GoString(assetDir))
	os.Setenv("xray.tun.fd", fmt.Sprint(int(tunFd)))

	raw := selectIndex([]byte(C.GoString(body)), int(index))
	obj, _, err := pickConfig(raw)
	if err != nil {
		return cstr(err.Error())
	}
	o := parseOpts(C.GoString(opts))
	addRouting(obj, o)
	if o.Lite {
		dropHeavyGeo(obj, o.BlockAds)
	}
	sanitizeGeo(obj, C.GoString(assetDir))
	cfg, err := buildTunConfig(obj)
	if err != nil {
		return cstr(err.Error())
	}
	config, err := core.LoadConfig("json", bytes.NewReader(cfg))
	if err != nil {
		return cstr("ошибка конфига: " + err.Error())
	}
	inst, err := core.New(config)
	if err != nil {
		return cstr("ошибка запуска: " + err.Error())
	}
	if err := inst.Start(); err != nil {
		return cstr("ошибка старта: " + err.Error())
	}
	instance = inst
	debug.FreeOSMemory()
	return cstr("")
}

// XKCheck validates that a subscription can be turned into a config,
// returns "" or error. Used by the app before saving.
//
//export XKCheck
func XKCheck(body *C.char) *C.char {
	obj, _, err := pickConfig([]byte(C.GoString(body)))
	if err != nil {
		return cstr(err.Error())
	}
	if _, err := buildTunConfig(obj); err != nil {
		return cstr(err.Error())
	}
	return cstr("")
}

// XKDelay measures real latency through the running tunnel: an HTTP request
// to generate_204 dialed via Xray's own routing (i.e. through the VPN server).
// Returns milliseconds, or -1 on failure.
//
//export XKDelay
func XKDelay() C.int {
	mu.Lock()
	inst := instance
	mu.Unlock()
	if inst == nil {
		return -1
	}
	ctx, cancel := context.WithTimeout(context.Background(), 6*time.Second)
	defer cancel()
	t0 := time.Now()
	conn, err := core.Dial(ctx, inst, xnet.TCPDestination(xnet.ParseAddress("www.gstatic.com"), xnet.Port(80)))
	if err != nil {
		return -1
	}
	defer conn.Close()
	_ = conn.SetDeadline(time.Now().Add(6 * time.Second))
	if _, err := conn.Write([]byte("GET /generate_204 HTTP/1.1\r\nHost: www.gstatic.com\r\nUser-Agent: XinkaliVPN\r\nConnection: close\r\n\r\n")); err != nil {
		return -1
	}
	buf := make([]byte, 12)
	if _, err := io.ReadAtLeast(conn, buf, 12); err != nil || !bytes.HasPrefix(buf, []byte("HTTP/1.")) {
		return -1
	}
	ms := int(time.Since(t0).Milliseconds())
	if ms < 1 {
		ms = 1
	}
	return C.int(ms)
}

//export XKStop
func XKStop() {
	mu.Lock()
	defer mu.Unlock()
	if instance != nil {
		_ = instance.Close()
		instance = nil
	}
	debug.FreeOSMemory()
}

func main() {}
