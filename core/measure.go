package main

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net"
	"net/http"
	"strings"
	"time"

	xnet "github.com/xtls/xray-core/common/net"
	"github.com/xtls/xray-core/core"
)

// measureConfig runs a throw-away Xray instance with only the server's outbounds (no routing, no
// TUN) and times an HTTPS request to url through it. The first request pays for the proxy + TLS
// handshakes; the second one reuses the connection and is the real round trip through the server.
func measureConfig(obj map[string]any, url string, timeout time.Duration) int {
	obs, _ := obj["outbounds"].([]any)
	var main any
	rest := []any{}
	for _, o := range obs {
		m, ok := o.(map[string]any)
		if !ok {
			continue
		}
		p, _ := m["protocol"].(string)
		if p == "freedom" || p == "blackhole" || p == "dns" || p == "loopback" {
			continue
		}
		if t, _ := m["tag"].(string); main == nil && (t == "proxy" || t == "") {
			main = m
			continue
		}
		rest = append(rest, m)
	}
	if main == nil {
		if len(rest) == 0 {
			return -1
		}
		main, rest = rest[0], rest[1:]
	}
	cfg := map[string]any{
		"log":       map[string]any{"loglevel": "none"},
		"outbounds": append([]any{main}, rest...),
	}
	raw, err := json.Marshal(cfg)
	if err != nil {
		return -1
	}
	config, err := core.LoadConfig("json", bytes.NewReader(raw))
	if err != nil {
		if dbg {
			fmt.Println("load:", err)
		}
		return -1
	}
	inst, err := core.New(config)
	if err != nil {
		if dbg {
			fmt.Println("new:", err)
		}
		return -1
	}
	if err := inst.Start(); err != nil {
		return -1
	}
	defer inst.Close()

	tr := &http.Transport{
		DialContext: func(ctx context.Context, network, addr string) (net.Conn, error) {
			dest, err := xnet.ParseDestination(network + ":" + addr)
			if err != nil {
				return nil, err
			}
			return core.Dial(ctx, inst, dest)
		},
		MaxIdleConns: 1, IdleConnTimeout: 10 * time.Second, ForceAttemptHTTP2: false,
	}
	defer tr.CloseIdleConnections()
	client := &http.Client{Transport: tr, Timeout: timeout,
		CheckRedirect: func(*http.Request, []*http.Request) error { return http.ErrUseLastResponse }}
	one := func() (int, bool) {
		req, _ := http.NewRequest("GET", url, nil)
		req.Header.Set("User-Agent", "Mozilla/5.0")
		t0 := time.Now()
		resp, err := client.Do(req)
		if err != nil {
			if dbg {
				fmt.Println("measure err:", err)
			}
			return -1, false
		}
		io.Copy(io.Discard, io.LimitReader(resp.Body, 64<<10))
		resp.Body.Close()
		ms := int(time.Since(t0).Milliseconds())
		if resp.StatusCode >= 500 {
			return -1, false
		}
		return ms, true
	}
	first, ok := one()
	if !ok {
		return -1
	}
	second, ok2 := one()
	ms := first
	if ok2 && second > 0 && second < first {
		ms = second
	}
	if ms < 1 {
		ms = 1
	}
	return ms
}

func measureSub(body []byte, index int, url string, timeoutMs int) int {
	obj, _, err := pickConfig(selectIndex(body, index))
	if err != nil {
		return -1
	}
	if url == "" {
		url = "https://www.gstatic.com/generate_204"
	}
	if !strings.HasPrefix(url, "http") {
		url = "https://" + url
	}
	if timeoutMs < 1000 {
		timeoutMs = 3000
	}
	done := make(chan int, 1)
	go func() { done <- measureConfig(obj, url, time.Duration(timeoutMs)*time.Millisecond) }()
	select {
	case ms := <-done:
		return ms
	case <-time.After(time.Duration(timeoutMs*2+1500) * time.Millisecond):
		return -1
	}
}

var dbg = false

var _ = fmt.Sprint
