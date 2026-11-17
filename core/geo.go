package main

import (
	"bufio"
	"encoding/binary"
	"io"
	"os"
	"path/filepath"
	"strings"
)

// geoCodes returns the set of codes in a geo .dat file by streaming it
// (reads only the code of each entry, skips bodies) to keep memory low.
func geoCodes(path string) map[string]bool {
	f, err := os.Open(path)
	if err != nil {
		return nil
	}
	defer f.Close()
	r := bufio.NewReaderSize(f, 64<<10)
	set := map[string]bool{}
	for {
		if _, err := binary.ReadUvarint(r); err != nil { // tag of entry
			break
		}
		n, err := binary.ReadUvarint(r) // entry length
		if err != nil {
			break
		}
		remain := int64(n)
		// inside entry: expect field 1 (code) first
		tag, err := binary.ReadUvarint(r)
		if err != nil {
			break
		}
		remain -= int64(uvlen(tag))
		if tag>>3 == 1 && tag&7 == 2 {
			l, err := binary.ReadUvarint(r)
			if err != nil {
				break
			}
			remain -= int64(uvlen(l))
			buf := make([]byte, l)
			if _, err := io.ReadFull(r, buf); err != nil {
				break
			}
			remain -= int64(l)
			set[strings.ToUpper(string(buf))] = true
		}
		if remain > 0 {
			if _, err := r.Discard(int(remain)); err != nil {
				break
			}
		}
	}
	return set
}

func uvlen(x uint64) int {
	n := 1
	for x >= 0x80 {
		x >>= 7
		n++
	}
	return n
}

// sanitizeGeo removes geoip:/geosite: entries whose codes are not in the
// bundled files, so a template referencing an unknown list still starts.
func sanitizeGeo(obj map[string]any, assetDir string) {
	sites := geoCodes(filepath.Join(assetDir, "geosite.dat"))
	ips := geoCodes(filepath.Join(assetDir, "geoip.dat"))
	ok := func(s string) bool {
		v := strings.TrimPrefix(s, "!")
		var set map[string]bool
		switch {
		case strings.HasPrefix(v, "geosite:"):
			set, v = sites, v[len("geosite:"):]
		case strings.HasPrefix(v, "geoip:"):
			set, v = ips, v[len("geoip:"):]
		case strings.HasPrefix(v, "ext:"):
			return false // external files are not bundled
		default:
			return true
		}
		if set == nil {
			return false
		}
		if i := strings.Index(v, "@"); i >= 0 {
			v = v[:i]
		}
		return set[strings.ToUpper(v)]
	}
	filter := func(list []any) []any {
		out := []any{}
		for _, e := range list {
			if s, isStr := e.(string); !isStr || ok(s) {
				out = append(out, e)
			}
		}
		return out
	}
	if routing, _ := obj["routing"].(map[string]any); routing != nil {
		rules, _ := routing["rules"].([]any)
		kept := []any{}
		for _, r := range rules {
			m, isMap := r.(map[string]any)
			if !isMap {
				continue
			}
			drop := false
			for _, key := range []string{"domain", "domains", "ip", "source", "sourceIP"} {
				if l, isList := m[key].([]any); isList {
					f := filter(l)
					if len(f) == 0 && len(l) > 0 {
						drop = true
					}
					m[key] = f
				}
			}
			if !drop {
				kept = append(kept, m)
			}
		}
		routing["rules"] = kept
	}
	if dns, _ := obj["dns"].(map[string]any); dns != nil {
		servers, _ := dns["servers"].([]any)
		for _, s := range servers {
			if m, isMap := s.(map[string]any); isMap {
				for _, key := range []string{"domains", "expectIPs", "expectedIPs", "unexpectedIPs"} {
					if l, isList := m[key].([]any); isList {
						m[key] = filter(l)
					}
				}
			}
		}
	}
}
