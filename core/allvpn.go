package main

// applyAllVpn — режим «Весь трафик через VPN»: правила подписки и личные исключения отбрасываются,
// остаются только торренты и локальная сеть (напрямую), блок рекламы (если включён) и общее правило «всё → VPN».
// proxyRule — balancerTag подписки или outboundTag первого прокси; пуст, если прокси в конфиге нет.
func applyAllVpn(obj map[string]any, routing map[string]any, o xkOpts, direct, block string, proxyRule map[string]any) {
	rules := []any{
		map[string]any{"type": "field", "protocol": []any{"bittorrent"}, "outboundTag": direct},
		map[string]any{"type": "field", "domain": []any{"geosite:torrent"}, "outboundTag": direct},
		map[string]any{"type": "field", "ip": []any{"geoip:private"}, "outboundTag": direct},
	}
	if o.BlockAds {
		rules = append(rules, map[string]any{"type": "field", "domain": []any{"geosite:category-ads", "geosite:win-spy"}, "outboundTag": block})
	}
	if len(proxyRule) > 0 {
		all := map[string]any{"type": "field", "network": "tcp,udp"}
		for k, v := range proxyRule {
			all[k] = v
		}
		rules = append(rules, all)
	}
	routing["rules"] = rules
	obj["routing"] = routing
}
