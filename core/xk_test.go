package main
import ("testing";"bytes";"strings";"github.com/xtls/xray-core/core")
const sample = `[{"remarks":"🇩🇪 Auto","log":{"loglevel":"warning"},"dns":{"servers":["1.1.1.1"]},
"inbounds":[{"tag":"socks","port":10808,"listen":"127.0.0.1","protocol":"socks","settings":{"udp":true}},{"tag":"http","port":10809,"protocol":"http"}],
"outbounds":[{"tag":"proxy","protocol":"vless","settings":{"vnext":[{"address":"1.2.3.4","port":443,"users":[{"id":"e5c1b1f2-1111-2222-3333-444455556666","encryption":"none","flow":"xtls-rprx-vision"}]}]},"streamSettings":{"network":"tcp","security":"reality","realitySettings":{"serverName":"x.com","publicKey":"Z84J2IelR9ch3k8VtlVhhs5ycBUlXA7wHBWcBrjqnAw","shortId":"a1b2","fingerprint":"chrome"}}},
{"tag":"direct","protocol":"freedom"},{"tag":"block","protocol":"blackhole"}],
"burstObservatory":{"subjectSelector":["proxy"],"pingConfig":{"destination":"http://www.gstatic.com/generate_204","interval":"1m","sampling":1,"timeout":"3s"}},
"routing":{"domainStrategy":"IPIfNonMatch","balancers":[{"tag":"bal","selector":["proxy"],"strategy":{"type":"leastLoad"},"fallbackTag":"proxy"}],
"rules":[{"type":"field","inboundTag":["socks","http"],"network":"tcp,udp","balancerTag":"bal"},{"type":"field","ip":["geoip:private"],"outboundTag":"direct"},{"type":"field","domain":["geosite:ru-blocked-all"],"outboundTag":"proxy"},{"type":"field","domain":["geosite:category-ru","geosite:nonexist"],"outboundTag":"direct"},{"type":"field","ip":["geoip:ru"],"outboundTag":"direct"},{"type":"field","protocol":["bittorrent"],"outboundTag":"direct"}]}}]`
func TestBuild(t *testing.T){
  obj,remark,err:=pickConfig([]byte(sample)); if err!=nil{t.Fatal(err)}
  addRouting(obj, xkOpts{RuDirect:true, BlockAds:true})
  sanitizeGeo(obj,"/root/ios/assets-trim")
  b,err:=buildTunConfig(obj); if err!=nil{t.Fatal(err)}
  t.Log(remark); t.Log(string(b))
  if _,err:=core.LoadConfig("json",bytes.NewReader(b)); err!=nil{t.Fatal(err)}
}

func TestVkusVillAlwaysDirect(t *testing.T){
  obj,_,_:=pickConfig([]byte(sample))
  addRouting(obj, xkOpts{RuDirect:false})
  sanitizeGeo(obj,"/root/ios/assets-trim")
  b,_:=buildTunConfig(obj)
  s:=string(b)
  if !strings.Contains(s, `"keyword:vkusvill"`) { t.Fatal("no vkusvill rule") }
  i:=strings.Index(s,`keyword:vkusvill`); j:=strings.Index(s,`"balancerTag":"bal","inboundTag"`)
  if i<0||j<0||i>j { t.Fatal("vkusvill rule must come before catch-all") }
  cfg,err:=core.LoadConfig("json",bytes.NewReader(b)); if err!=nil{t.Fatal(err)}
  _ = cfg
  t.Log(s[strings.Index(s,`"dns"`):strings.Index(s,`"dns"`)+200])
}

func TestUserRouting(t *testing.T){
  obj,_,_:=pickConfig([]byte(sample))
  addRouting(obj, xkOpts{RuDirect:false, Cats:[]string{"banks","games"}, Domains:[]string{"https://Example.com/path","*.foo.org"}, Apps:[]string{"Telegram.exe"}})
  sanitizeGeo(obj,"/root/ios/assets-trim")
  b,_:=buildTunConfig(obj)
  s:=string(b)
  for _,want:=range []string{`"domain:example.com"`,`"domain:foo.org"`,`"process":["Telegram.exe"]`,`geosite:category-bank-ru`} {
    if !strings.Contains(s,want){ t.Fatal("missing "+want) }
  }
  if _,err:=core.LoadConfig("json",bytes.NewReader(b)); err!=nil{t.Fatal(err)}
}

func TestLite(t *testing.T){
  obj,_,_:=pickConfig([]byte(sample))
  o:=xkOpts{RuDirect:true, Lite:true}
  addRouting(obj,o); dropHeavyGeo(obj,false); sanitizeGeo(obj,"/root/ios/assets-trim")
  b,_:=buildTunConfig(obj); s:=string(b)
  if strings.Contains(s,"ru-blocked") { t.Fatal("heavy list still present") }
  if _,err:=core.LoadConfig("json",bytes.NewReader(b)); err!=nil{t.Fatal(err)}
}

func TestNoVV(t *testing.T){
  obj,_,_:=pickConfig([]byte(sample))
  addRouting(obj, xkOpts{RuDirect:false, NoVV:true}); sanitizeGeo(obj,"/root/ios/assets-trim")
  b,_:=buildTunConfig(obj); s:=string(b)
  if strings.Contains(s,"vkusvill") { t.Fatal("vv present") }
  if _,err:=core.LoadConfig("json",bytes.NewReader(b)); err!=nil{t.Fatal(err)}
}

func TestAllVpn(t *testing.T){
  obj,_,_:=pickConfig([]byte(sample))
  addRouting(obj, xkOpts{AllVpn:true, Cats:[]string{"ru"}, Domains:[]string{"example.com"}})
  sanitizeGeo(obj,"/root/ios/assets-trim")
  b,err:=buildTunConfig(obj); if err!=nil{t.Fatal(err)}
  s:=string(b)
  for _,bad:=range []string{"vkusvill","category-ru","geoip:ru","example.com","77.88.8.8"}{
    if strings.Contains(s,bad){ t.Fatalf("rule %q must be dropped in all-VPN mode", bad) }
  }
  if !strings.Contains(s,"bittorrent"){ t.Fatal("torrents must stay direct") }
  if !strings.Contains(s,`"balancerTag":"bal"`){ t.Fatal("no catch-all to proxy") }
  if _,err:=core.LoadConfig("json",bytes.NewReader(b)); err!=nil{t.Fatal(err)}
}
