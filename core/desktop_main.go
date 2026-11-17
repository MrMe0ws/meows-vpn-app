//go:build !android && !cgo

// Desktop (Windows): режим "tun" (wintun, админ) или "proxy" — локальный SOCKS5 + HTTP на 127.0.0.1 (приложение включает системный прокси).
//   meows-core.exe <config.json> <asset-dir>                      запуск ядра
//   meows-core.exe measure <sub-file> <url> <timeout-ms> <i,j,k>  реальный пинг серверов
// Родитель держит stdin открытым: закрылся stdin (приложение закрыто/упало) — ядро завершается само.
package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"os"
	"os/signal"
	"runtime/debug"
	"syscall"

	"github.com/xtls/xray-core/core"
	_ "github.com/xtls/xray-core/main/distro/all"
)

type desktopCfg struct {
	Sub       string `json:"sub"`
	Index     int    `json:"index"`
	Opts      string `json:"opts"`
	SocksPort int    `json:"socksPort"`
	HttpPort  int    `json:"httpPort"`
	Traffic   bool   `json:"traffic"` // журнал соединений (куда и через какой выход) в stdout
	Mode      string `json:"mode"` // "tun" — адаптер wintun (нужен админ), иначе локальные прокси
}

func main() {
	if len(os.Args) >= 2 && os.Args[1] == "measure" {
		dbg = os.Getenv("XKDBG") != ""
		runMeasure()
		return
	}
	if len(os.Args) < 3 {
		fail("usage: meows-core <config.json> <asset-dir>")
	}
	debug.SetGCPercent(50)
	raw, err := os.ReadFile(os.Args[1])
	if err != nil {
		fail("config: " + err.Error())
	}
	var dc desktopCfg
	if err := json.Unmarshal(raw, &dc); err != nil {
		fail("config json: " + err.Error())
	}
	if dc.SocksPort == 0 {
		dc.SocksPort = 10808
	}
	if dc.HttpPort == 0 {
		dc.HttpPort = 10809
	}
	os.Setenv("xray.location.asset", os.Args[2])

	obj, _, err := pickConfig(selectIndex([]byte(dc.Sub), dc.Index))
	if err != nil {
		fail(err.Error())
	}
	addRouting(obj, parseOpts(dc.Opts))
	sanitizeGeo(obj, os.Args[2])
	var cfg []byte
	if dc.Mode == "tun" {
		cfg, err = buildWinTunConfig(obj)
	} else {
		cfg, err = buildProxyConfig(obj, dc.SocksPort, dc.HttpPort)
	}
	if err != nil {
		fail(err.Error())
	}
	if dc.Traffic {
		cfg = withStats(withAccessLog(cfg))
	}
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

	stop := make(chan struct{}, 1)
	go func() { _, _ = io.Copy(io.Discard, os.Stdin); stop <- struct{}{} }()
	sig := make(chan os.Signal, 1)
	signal.Notify(sig, syscall.SIGTERM, syscall.SIGINT)
	select {
	case <-sig:
	case <-stop:
	}
	inst.Close()
}