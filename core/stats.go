package main

// Счётчики трафика для экрана «Статистика» (Android и Windows). Раз в секунду печатаем накопленные байты:
//   XKSTAT <через VPN: отправлено> <через VPN: получено> <напрямую: отправлено> <напрямую: получено>
// Родитель считает скорость как разницу соседних строк. Замер скорости (нагрузка на сервер) не делаем — только то, что реально прошло.

import (
	"encoding/json"
	"fmt"
	"strings"
	"time"

	"github.com/xtls/xray-core/core"
	"github.com/xtls/xray-core/features/stats"
)

// withStats включает счётчики исходящих соединений в конфиге (policy.system.statsOutbound*).
func withStats(cfg []byte) []byte {
	var obj map[string]any
	if json.Unmarshal(cfg, &obj) != nil {
		return cfg
	}
	obj["stats"] = map[string]any{}
	pol, _ := obj["policy"].(map[string]any)
	if pol == nil {
		pol = map[string]any{}
	}
	sys, _ := pol["system"].(map[string]any)
	if sys == nil {
		sys = map[string]any{}
	}
	sys["statsOutboundUplink"] = true
	sys["statsOutboundDownlink"] = true
	pol["system"] = sys
	obj["policy"] = pol
	if out, err := json.Marshal(obj); err == nil {
		return out
	}
	return cfg
}

// statsLoop печатает XKSTAT каждую секунду, пока работает процесс.
func statsLoop(inst *core.Instance) {
	sm, _ := inst.GetFeature(stats.ManagerType()).(stats.Manager)
	if sm == nil {
		return
	}
	for range time.Tick(time.Second) {
		var vUp, vDown, dUp, dDown int64
		sm.VisitCounters(func(name string, c stats.Counter) bool {
			// outbound>>>TAG>>>traffic>>>uplink|downlink
			p := strings.Split(name, ">>>")
			if len(p) != 4 || p[0] != "outbound" || p[2] != "traffic" {
				return true
			}
			tag := strings.ToLower(p[1])
			v := c.Value()
			switch {
			case strings.Contains(tag, "dns") || strings.Contains(tag, "block") || strings.Contains(tag, "blackhole"):
			case strings.Contains(tag, "direct") || strings.Contains(tag, "freedom"):
				if p[3] == "uplink" {
					dUp += v
				} else {
					dDown += v
				}
			default:
				if p[3] == "uplink" {
					vUp += v
				} else {
					vDown += v
				}
			}
			return true
		})
		fmt.Printf("XKSTAT %d %d %d %d\n", vUp, vDown, dUp, dDown)
	}
}
