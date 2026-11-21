package main

import (
	"encoding/json"
	"strings"
)

// Профиль маршрутизации в формате Happ/INCY (`happ://routing/add/<base64 JSON>`): списки «напрямую / через VPN / блок»,
// порядок правил, стратегия доменов. Если профиль выбран пользователем, его правила ЗАМЕНЯЮТ правила шаблона подписки
// (личные надстройки приложения остаются выше, см. addRouting). Без профиля поведение прежнее — правила подписки.
// DNS-блок (RemoteDNS* — для трафика через VPN, DomesticDNS* — для прямых доменов) переводится в dns.servers; если он пуст,
// DNS остаётся из подписки. DoT (tls://) наше ядро Xray не поддерживает, поэтому типы: DoH, DoU (обычный UDP), TCP.
type routeProfile struct {
	GlobalProxy    any      `json:"GlobalProxy"` // true — всё, что не попало в списки, идёт через VPN; false — напрямую
	DirectSites    []string `json:"DirectSites"`
	DirectIp       []string `json:"DirectIp"`
	ProxySites     []string `json:"ProxySites"`
	ProxyIp        []string `json:"ProxyIp"`
	BlockSites     []string `json:"BlockSites"`
	BlockIp        []string `json:"BlockIp"`
	DomainStrategy string   `json:"DomainStrategy"` // AsIs | IPIfNonMatch | IPOnDemand
	RouteOrder     string   `json:"RouteOrder"`     // например block-proxy-direct

	RemoteDNSType   string `json:"RemoteDNSType"` // DoH | DoU | TCP
	RemoteDNSDomain string `json:"RemoteDNSDomain"` // DoH: URL https://…/dns-query
	RemoteDNSIP     string `json:"RemoteDNSIP"`     // DoU/TCP: адрес сервера
	DomesticDNSType   string `json:"DomesticDNSType"`
	DomesticDNSDomain string `json:"DomesticDNSDomain"`
	DomesticDNSIP     string `json:"DomesticDNSIP"`
	DnsHosts          map[string]string `json:"DnsHosts"` // домен → IP, идёт в dns.hosts
}

// flexBool понимает и true, и "true" (Happ пишет флаги строками).
func flexBool(v any, def bool) bool {
	switch x := v.(type) {
	case bool:
		return x
	case string:
		switch strings.ToLower(strings.TrimSpace(x)) {
		case "true", "1":
			return true
		case "false", "0":
			return false
		}
	}
	return def
}

func parseProfile(s string) (*routeProfile, bool) {
	if strings.TrimSpace(s) == "" {
		return nil, false
	}
	var p routeProfile
	if json.Unmarshal([]byte(s), &p) != nil {
		return nil, false
	}
	return &p, true
}

func cleanList(l []string) []any {
	out := []any{}
	for _, e := range l {
		if e = strings.TrimSpace(e); e != "" {
			out = append(out, e)
		}
	}
	return out
}

// profileOrder — порядок групп; нераспознанное значение даёт порядок по умолчанию «блок → прокси → напрямую».
func profileOrder(s string) []string {
	def := []string{"block", "proxy", "direct"}
	parts := strings.Split(strings.ToLower(strings.TrimSpace(s)), "-")
	if len(parts) != 3 {
		return def
	}
	seen := map[string]bool{}
	for _, p := range parts {
		if p != "block" && p != "proxy" && p != "direct" || seen[p] {
			return def
		}
		seen[p] = true
	}
	return parts
}

// profileRules строит правила Xray из профиля. proxyRule — balancerTag/outboundTag прокси подписки (пуст, если прокси нет).
// Локальная сеть всегда напрямую. Возвращает правила и стратегию доменов (пустую — оставить как в подписке).
func profileRules(p *routeProfile, direct, block string, proxyRule map[string]any) ([]any, string) {
	rules := []any{map[string]any{"type": "field", "ip": []any{"geoip:private"}, "outboundTag": direct}}
	add := func(domain, ip []any, target map[string]any) {
		for _, f := range []struct {
			key  string
			list []any
		}{{"domain", domain}, {"ip", ip}} {
			if len(f.list) == 0 {
				continue
			}
			r := map[string]any{"type": "field", f.key: f.list}
			for k, v := range target {
				r[k] = v
			}
			rules = append(rules, r)
		}
	}
	for _, g := range profileOrder(p.RouteOrder) {
		switch g {
		case "block":
			add(cleanList(p.BlockSites), cleanList(p.BlockIp), map[string]any{"outboundTag": block})
		case "proxy":
			if len(proxyRule) > 0 {
				add(cleanList(p.ProxySites), cleanList(p.ProxyIp), proxyRule)
			}
		case "direct":
			add(cleanList(p.DirectSites), cleanList(p.DirectIp), map[string]any{"outboundTag": direct})
		}
	}
	rest := map[string]any{"type": "field", "network": "tcp,udp"}
	if flexBool(p.GlobalProxy, true) && len(proxyRule) > 0 {
		for k, v := range proxyRule {
			rest[k] = v
		}
	} else {
		rest["outboundTag"] = direct
	}
	rules = append(rules, rest)

	strategy := ""
	switch strings.ToLower(p.DomainStrategy) {
	case "asis":
		strategy = "AsIs"
	case "ipifnonmatch":
		strategy = "IPIfNonMatch"
	case "ipondemand":
		strategy = "IPOnDemand"
	}
	return rules, strategy
}

// dnsAddress собирает адрес сервера Xray: local — запросы идут напрямую (https+local, tcp+local), иначе через VPN.
func dnsAddress(typ, domain, ip string, local bool) string {
	domain, ip = strings.TrimSpace(domain), strings.TrimSpace(ip)
	suffix := ""
	if local {
		suffix = "+local"
	}
	switch strings.ToUpper(strings.TrimSpace(typ)) {
	case "DOH":
		if domain == "" {
			return ""
		}
		if !strings.Contains(domain, "://") {
			domain = "https://" + domain + "/dns-query"
		}
		return "https" + suffix + "://" + domain[strings.Index(domain, "://")+3:]
	case "TCP":
		if ip == "" {
			return ""
		}
		return "tcp" + suffix + "://" + strings.TrimPrefix(ip, "tcp://")
	case "DOU":
		return ip
	}
	return ""
}

// profileDNS строит dns.servers из профиля: прямые домены — через «домашний» DNS, остальное — через удалённый.
// Возвращает nil, если в профиле нет DNS-настроек (тогда DNS остаётся из подписки). rest — серверы подписки на запас.
func profileDNS(p *routeProfile, rest []any) []any {
	remote := dnsAddress(p.RemoteDNSType, p.RemoteDNSDomain, p.RemoteDNSIP, false)
	local := dnsAddress(p.DomesticDNSType, p.DomesticDNSDomain, p.DomesticDNSIP, true)
	servers := []any{}
	if direct := cleanList(p.DirectSites); local != "" && len(direct) > 0 {
		servers = append(servers, map[string]any{"address": local, "domains": direct, "skipFallback": true})
	}
	if remote != "" {
		servers = append(servers, remote)
	} else if len(servers) > 0 {
		servers = append(servers, rest...)
	}
	if len(servers) == 0 {
		return nil
	}
	return servers
}
