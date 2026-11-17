package main

import (
	"encoding/json"
	"strings"
)

type xkOpts struct {
	RuDirect bool     `json:"ruDirect"`
	BlockAds bool     `json:"blockAds"`
	Cats     []string `json:"cats"`    // categories that bypass the VPN
	Domains  []string `json:"domains"` // user sites that bypass the VPN
	Apps     []string `json:"apps"`    // desktop: process names that bypass the VPN
	Lite     bool     `json:"lite"`
	NoVV     bool     `json:"noVV"`    // white-label build: no built-in VkusVill bypass    // iOS: ~50 MB extension limit — skip the heaviest geo lists
}

// heavy geosite lists (several MB of matchers each) dropped in lite mode
var heavyGeo = map[string]bool{"ru-blocked": true, "ru-blocked-all": true, "refilter": true, "antifilter-download": true}

// dropHeavyGeo removes the biggest geosite lists from rules and DNS so the
// iOS extension stays well under its memory limit (avoids silent kills).
func dropHeavyGeo(obj map[string]any, keepAds bool) {
	heavy := func(v string) bool {
		v = strings.ToLower(strings.TrimPrefix(v, "!"))
		if !strings.HasPrefix(v, "geosite:") {
			return false
		}
		code := strings.SplitN(v[8:], "@", 2)[0]
		if code == "category-ads-all" && !keepAds {
			return true
		}
		return heavyGeo[code]
	}
	filter := func(l []any) []any {
		out := []any{}
		for _, e := range l {
			if s, ok := e.(string); ok && heavy(s) {
				continue
			}
			out = append(out, e)
		}
		return out
	}
	if routing, _ := obj["routing"].(map[string]any); routing != nil {
		rules, _ := routing["rules"].([]any)
		kept := []any{}
		for _, r := range rules {
			m, ok := r.(map[string]any)
			if !ok {
				continue
			}
			if l, ok := m["domain"].([]any); ok {
				f := filter(l)
				if len(f) == 0 && len(l) > 0 {
					continue
				}
				m["domain"] = f
			}
			kept = append(kept, m)
		}
		routing["rules"] = kept
	}
	if dns, _ := obj["dns"].(map[string]any); dns != nil {
		servers, _ := dns["servers"].([]any)
		for _, s := range servers {
			if m, ok := s.(map[string]any); ok {
				if l, ok := m["domains"].([]any); ok {
					m["domains"] = filter(l)
				}
			}
		}
	}
}

// Categories the user can send around the VPN.
var categoryDomains = map[string][]any{
	"banks":  {"geosite:category-bank-ru", "geosite:tbank-ru"},
	"gov":    {"geosite:category-gov-ru"},
	"shops":  {"geosite:category-ecommerce-ru", "geosite:category-retail-ru"},
	"yandex": {"geosite:yandex"},
	"vk":     {"geosite:vk", "geosite:mailru", "geosite:mailru-group"},
	"media":  {"geosite:category-media-ru", "geosite:category-entertainment-ru", "geosite:rutube"},
	"apple":  {"geosite:apple"},
	"microsoft": {"geosite:microsoft"},
	"games":  {"geosite:category-games", "geosite:steam", "geosite:epicgames"},
}

func userDomain(d string) string {
	d = strings.TrimSpace(strings.ToLower(d))
	d = strings.TrimPrefix(strings.TrimPrefix(d, "https://"), "http://")
	if i := strings.IndexAny(d, "/?#"); i >= 0 {
		d = d[:i]
	}
	d = strings.TrimPrefix(d, "*.")
	if d == "" {
		return ""
	}
	if strings.Contains(d, ":") && !strings.HasPrefix(d, "domain:") && !strings.HasPrefix(d, "full:") && !strings.HasPrefix(d, "keyword:") && !strings.HasPrefix(d, "regexp:") && !strings.HasPrefix(d, "geosite:") {
		return ""
	}
	if strings.Contains(d, ":") {
		return d
	}
	return "domain:" + d
}

func parseOpts(s string) xkOpts {
	o := xkOpts{RuDirect: true}
	if strings.TrimSpace(s) != "" {
		_ = json.Unmarshal([]byte(s), &o)
	}
	return o
}

var ruDirectDomains = []any{
	"geosite:category-ru", "geosite:ru-available-only-inside", "geosite:tld-ru",
	"geosite:yandex", "geosite:vk", "geosite:mailru", "geosite:mailru-group",
	"geosite:category-gov-ru", "geosite:category-bank-ru", "geosite:category-retail-ru",
	"geosite:category-ecommerce-ru", "geosite:category-media-ru", "geosite:category-entertainment-ru",
	"geosite:category-education-ru", "geosite:category-medicine-ru", "geosite:category-travel-ru",
	"geosite:tbank-ru", "geosite:mts-ru", "geosite:t2-ru", "geosite:rutube", "geosite:autoru",
	"geosite:myoffice-ru", "geosite:category-ai-ru",
	`regexp:\.ru$`, `regexp:\.su$`, `regexp:\.xn--p1ai$`, `regexp:\.xn--d1acj3b$`,
	`regexp:\.moscow$`, `regexp:\.tatar$`, `regexp:\.xn--80adxhks$`,
}

// addRouting injects our routing on top of the subscription rules:
//  1. ads -> block (optional)
//  2. sites blocked in RU (even on .ru) -> proxy
//  3. Russian domains & zones -> direct
//  4. Russian & private IPs -> direct
// Domains that must ALWAYS bypass the VPN (VkusVill: client app, courier
// app, site, CDNs), regardless of user settings.
var alwaysDirectDomains = []any{
	"keyword:vkusvill", "keyword:izbenka",
	"domain:vv.ru", "domain:vkusvill.ru",
}

func addRouting(obj map[string]any, o xkOpts) {
	outs, _ := obj["outbounds"].([]any)
	direct, block, firstProxy := "", "", ""
	for _, x := range outs {
		m, ok := x.(map[string]any)
		if !ok {
			continue
		}
		tag, _ := m["tag"].(string)
		switch m["protocol"] {
		case "freedom":
			if direct == "" && tag != "" {
				direct = tag
			}
		case "blackhole":
			if block == "" && tag != "" {
				block = tag
			}
		case "dns", "loopback":
		default:
			if firstProxy == "" && tag != "" {
				firstProxy = tag
			}
		}
	}
	if direct == "" {
		direct = "xk-direct"
		outs = append(outs, map[string]any{"protocol": "freedom", "tag": direct})
	}
	if block == "" && o.BlockAds {
		block = "xk-block"
		outs = append(outs, map[string]any{"protocol": "blackhole", "tag": block})
	}
	obj["outbounds"] = outs

	routing, _ := obj["routing"].(map[string]any)
	if routing == nil {
		routing = map[string]any{}
	}
	// proxy target: the balancer the subscription uses, else first proxy outbound
	proxyRule := map[string]any{}
	if bals, ok := routing["balancers"].([]any); ok && len(bals) > 0 {
		if b, ok := bals[0].(map[string]any); ok {
			if t, _ := b["tag"].(string); t != "" {
				proxyRule["balancerTag"] = t
			}
		}
	}
	if len(proxyRule) == 0 && firstProxy != "" {
		proxyRule["outboundTag"] = firstProxy
	}

	var pre []any
	// 0. VkusVill always direct, above everything else
	if !o.NoVV {
		pre = append(pre, map[string]any{"type": "field", "domain": alwaysDirectDomains, "outboundTag": direct})
	}
	pre = append(pre, map[string]any{"type": "field", "ip": []any{"77.88.8.8/32"}, "outboundTag": direct})
	// user choices: programs, own sites, categories -> direct
	if len(o.Apps) > 0 {
		apps := []any{}
		for _, a := range o.Apps {
			if a = strings.TrimSpace(a); a != "" {
				apps = append(apps, a)
			}
		}
		if len(apps) > 0 {
			pre = append(pre, map[string]any{"type": "field", "process": apps, "outboundTag": direct})
		}
	}
	userDoms := []any{}
	for _, d := range o.Domains {
		if u := userDomain(d); u != "" {
			userDoms = append(userDoms, u)
		}
	}
	for _, c := range o.Cats {
		userDoms = append(userDoms, categoryDomains[c]...)
	}
	if len(userDoms) > 0 {
		pre = append(pre, map[string]any{"type": "field", "domain": userDoms, "outboundTag": direct})
	}
	if o.BlockAds {
		pre = append(pre, map[string]any{"type": "field", "domain": []any{"geosite:category-ads-all"}, "outboundTag": block})
	}
	if o.RuDirect {
		if len(proxyRule) > 0 && !o.Lite {
			r := map[string]any{"type": "field", "domain": []any{"geosite:ru-blocked"}}
			for k, v := range proxyRule {
				r[k] = v
			}
			pre = append(pre, r)
		}
		pre = append(pre,
			map[string]any{"type": "field", "domain": ruDirectDomains, "outboundTag": direct},
			map[string]any{"type": "field", "ip": []any{"geoip:ru", "geoip:private"}, "outboundTag": direct},
		)
	}
	rules, _ := routing["rules"].([]any)
	routing["rules"] = append(pre, rules...)
	obj["routing"] = routing

	// DNS: resolve direct domains via Yandex DNS (reached directly: geoip:ru)
	dnsDomains := []any{}
	if !o.NoVV {
		dnsDomains = append(dnsDomains, alwaysDirectDomains...)
	}
	dnsDomains = append(dnsDomains, userDoms...)
	if o.RuDirect {
		dnsDomains = append(dnsDomains, ruDirectDomains...)
	}
	dns, _ := obj["dns"].(map[string]any)
	if dns == nil {
		dns = map[string]any{"servers": []any{"1.1.1.1", "8.8.8.8"}}
	}
	servers, _ := dns["servers"].([]any)
	if len(servers) == 0 {
		servers = []any{"1.1.1.1", "8.8.8.8"}
	}
	if len(dnsDomains) > 0 {
		servers = append([]any{map[string]any{
			"address": "77.88.8.8", "domains": dnsDomains, "skipFallback": true,
		}}, servers...)
	}
	dns["servers"] = servers
	obj["dns"] = dns
}
