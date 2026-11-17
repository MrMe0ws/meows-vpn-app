package main

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"strings"
)

// pickConfig takes the raw subscription body (Xray JSON object, JSON array of
// configs, or base64 of either) and returns one config object plus its remark.
func pickConfig(raw []byte) (map[string]any, string, error) {
	raw = bytes.TrimSpace(raw)
	if len(raw) > 0 && raw[0] != '{' && raw[0] != '[' {
		if dec, err := base64.StdEncoding.DecodeString(string(raw)); err == nil {
			raw = bytes.TrimSpace(dec)
		} else if dec, err := base64.RawStdEncoding.DecodeString(string(raw)); err == nil {
			raw = bytes.TrimSpace(dec)
		}
	}
	if len(raw) == 0 {
		return nil, "", fmt.Errorf("пустой ответ подписки")
	}
	var obj map[string]any
	if raw[0] == '[' {
		var arr []map[string]any
		if err := json.Unmarshal(raw, &arr); err != nil {
			return nil, "", fmt.Errorf("не удалось разобрать JSON подписки: %v", err)
		}
		if len(arr) == 0 {
			return nil, "", fmt.Errorf("в подписке нет серверов")
		}
		obj = arr[0]
	} else if raw[0] == '{' {
		if err := json.Unmarshal(raw, &obj); err != nil {
			return nil, "", fmt.Errorf("не удалось разобрать JSON подписки: %v", err)
		}
	} else {
		return nil, "", fmt.Errorf("подписка не в формате Xray JSON (включите Xray JSON шаблон в Remnawave)")
	}
	remark, _ := obj["remarks"].(string)
	return obj, remark, nil
}

func selectIndex(raw []byte, index int) []byte {
	raw = bytes.TrimSpace(raw)
	if len(raw) > 0 && raw[0] != '{' && raw[0] != '[' {
		if dec, err := base64.StdEncoding.DecodeString(string(raw)); err == nil {
			raw = bytes.TrimSpace(dec)
		}
	}
	if len(raw) > 0 && raw[0] == '[' && index > 0 {
		var arr []json.RawMessage
		if json.Unmarshal(raw, &arr) == nil && index < len(arr) {
			return arr[index]
		}
	}
	return raw
}

// buildTunConfig rewrites a client config for NetworkExtension use:
// inbounds -> one TUN inbound, DNS port 53 -> dns outbound, no API/stats.
func buildTunConfig(obj map[string]any) ([]byte, error) {
	obj["inbounds"] = []any{map[string]any{
		"tag":      "tun-in",
		"protocol": "tun",
		"settings": map[string]any{"name": "xray0", "MTU": 1500},
		"sniffing": map[string]any{
			"enabled":      true,
			"destOverride": []any{"http", "tls", "quic"},
			"routeOnly":    true,
		},
	}}
	delete(obj, "api")
	delete(obj, "stats")
	delete(obj, "metrics")
	delete(obj, "reverse")

	if lg, ok := obj["log"].(map[string]any); ok {
		lg["access"] = "none"
		lg["error"] = ""
		lg["loglevel"] = "warning"
	} else {
		obj["log"] = map[string]any{"loglevel": "warning", "access": "none"}
	}

	outs, _ := obj["outbounds"].([]any)
	hasDNS := false
	for _, o := range outs {
		if m, ok := o.(map[string]any); ok && m["tag"] == "xk-dns-out" {
			hasDNS = true
		}
	}
	if !hasDNS {
		outs = append(outs, map[string]any{"protocol": "dns", "tag": "xk-dns-out"})
	}
	obj["outbounds"] = outs

	routing, _ := obj["routing"].(map[string]any)
	if routing == nil {
		routing = map[string]any{}
	}
	rules, _ := routing["rules"].([]any)
	newRules := []any{map[string]any{
		"type":        "field",
		"inboundTag":  []any{"tun-in"},
		"port":        "53",
		"outboundTag": "xk-dns-out",
	}}
	for _, r := range rules {
		m, ok := r.(map[string]any)
		if !ok {
			continue
		}
		// drop rules bound to API or old local inbounds; retarget to tun
		if tags, ok := m["inboundTag"].([]any); ok {
			api := false
			for _, t := range tags {
				if s, _ := t.(string); strings.EqualFold(s, "api") {
					api = true
				}
			}
			if api {
				continue
			}
			m["inboundTag"] = []any{"tun-in"}
		}
		if s, _ := m["outboundTag"].(string); strings.EqualFold(s, "api") {
			continue
		}
		newRules = append(newRules, m)
	}
	routing["rules"] = newRules
	obj["routing"] = routing

	if _, ok := obj["dns"]; !ok {
		obj["dns"] = map[string]any{"servers": []any{"1.1.1.1", "8.8.8.8"}}
	}
	return json.Marshal(obj)
}

// buildWinTunConfig — TUN для Windows (wintun.dll рядом с exe, нужны права администратора): тот же конфиг, что и на Android,
// но адаптер создаёт и настраивает само ядро: маршруты в TUN, свои сокеты привязаны к физическому интерфейсу (autoOutboundsInterface).
func buildWinTunConfig(obj map[string]any) ([]byte, error) {
	raw, err := buildTunConfig(obj)
	if err != nil {
		return nil, err
	}
	var cfg map[string]any
	if err := json.Unmarshal(raw, &cfg); err != nil {
		return nil, err
	}
	ins, _ := cfg["inbounds"].([]any)
	if len(ins) == 1 {
		if in, ok := ins[0].(map[string]any); ok {
			in["settings"] = map[string]any{
				"name":                   "MeowsTun",
				"desc":                   "Meows",
				"mtu":                    1500,
				"gateway":                []any{"10.255.0.1/30"},
				"dns":                    []any{"1.1.1.1", "8.8.8.8"},
				"autoSystemRoutingTable": []any{"0.0.0.0/0"},
				"autoSystemWfpBlockLeak": []any{"dns"},
			}
		}
	}
	return json.Marshal(cfg)
}

// buildProxyConfig rewrites a client config for desktop use: inbounds -> local SOCKS5 + HTTP on 127.0.0.1
// (the app switches the system proxy to the HTTP one), no API/stats.
func buildProxyConfig(obj map[string]any, socksPort, httpPort int) ([]byte, error) {
	sniff := func() map[string]any {
		return map[string]any{"enabled": true, "destOverride": []any{"http", "tls", "quic"}, "routeOnly": true}
	}
	obj["inbounds"] = []any{
		map[string]any{"tag": "socks-in", "listen": "127.0.0.1", "port": socksPort, "protocol": "socks",
			"settings": map[string]any{"udp": true, "auth": "noauth"}, "sniffing": sniff()},
		map[string]any{"tag": "http-in", "listen": "127.0.0.1", "port": httpPort, "protocol": "http",
			"settings": map[string]any{}, "sniffing": sniff()},
	}
	delete(obj, "api")
	delete(obj, "stats")
	delete(obj, "metrics")
	delete(obj, "reverse")
	if lg, ok := obj["log"].(map[string]any); ok {
		lg["access"] = "none"
		lg["error"] = ""
		lg["loglevel"] = "warning"
	} else {
		obj["log"] = map[string]any{"loglevel": "warning", "access": "none"}
	}
	routing, _ := obj["routing"].(map[string]any)
	if routing == nil {
		routing = map[string]any{}
	}
	rules, _ := routing["rules"].([]any)
	kept := []any{}
	for _, r := range rules {
		m, ok := r.(map[string]any)
		if !ok {
			continue
		}
		if tags, ok := m["inboundTag"].([]any); ok {
			api := false
			for _, t := range tags {
				if s, _ := t.(string); strings.EqualFold(s, "api") {
					api = true
				}
			}
			if api {
				continue
			}
		}
		if s, _ := m["outboundTag"].(string); strings.EqualFold(s, "api") {
			continue
		}
		kept = append(kept, m)
	}
	routing["rules"] = kept
	obj["routing"] = routing
	if _, ok := obj["dns"]; !ok {
		obj["dns"] = map[string]any{"servers": []any{"1.1.1.1", "8.8.8.8"}}
	}
	return json.Marshal(obj)
}