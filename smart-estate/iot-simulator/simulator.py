"""
Standalone device-fleet simulator for the Smart Estate IoT layer. Stands in for the
physical sensors and Octopus card readers of a real estate: publishes MQTT messages the
backend subscribes to, and serves a control panel to trip sensors, open/close doors,
tap cards, or run ambient background traffic.

Run:  ./run.sh   (serves the control panel on http://localhost:8090)
"""
import json
import threading
import time
from contextlib import asynccontextmanager

import paho.mqtt.client as mqtt
from fastapi import FastAPI
from fastapi.responses import HTMLResponse
from pydantic import BaseModel

BROKER_HOST = "127.0.0.1"
BROKER_PORT = 1883

# --- device catalogue (matches the seeded estate) --------------------------
FIRE_SENSORS = [
    {"code": "SMK-AZ-1-01", "label": "Smoke · Azalea 1/F-01 (in-flat)"},
    {"code": "SMK-AZ-1-COR", "label": "Smoke · Azalea 1/F corridor"},
    {"code": "SMK-AZ-5-03", "label": "Smoke · Azalea 5/F-03 (in-flat)"},
    {"code": "SMK-AZ-LOBBY", "label": "Smoke · Azalea lobby"},
    {"code": "SMK-BA-LOBBY", "label": "Smoke · Bauhinia lobby"},
]
FIRE_DOORS = [
    {"code": "FD-AZ-1", "label": "Fire door · Azalea 1/F staircase"},
    {"code": "FD-AZ-5", "label": "Fire door · Azalea 5/F staircase"},
    {"code": "FD-BA-G", "label": "Fire door · Bauhinia ground staircase"},
]
NORMAL_DOORS = [
    {"code": "DR-AZ-REFUSE", "label": "Door · Azalea refuse room"},
]
WATER_SENSORS = [
    {"code": "WTR-AZ-1-01", "label": "Water leak · Azalea 1/F-01"},
]
# Octopus readers: every tower entrance, plus a lift and a flat door.
ENTRANCES = [
    {"code": "AP-AZ-ENT", "label": "Azalea entrance"},
    {"code": "AP-BA-ENT", "label": "Bauhinia entrance"},
    {"code": "AP-CA-ENT", "label": "Camellia entrance"},
    {"code": "AP-DA-ENT", "label": "Dahlia entrance"},
    {"code": "AP-ER-ENT", "label": "Erica entrance"},
    {"code": "AP-FR-ENT", "label": "Freesia entrance"},
]
READERS = ENTRANCES + [
    {"code": "AP-AZ-LIFT", "label": "Azalea elevator"},
]
CARDS = [
    {"number": "88220011", "label": "Chan Tai Man · Azalea 5/F-03"},
    {"number": "88220012", "label": "Chan Siu Ling · Azalea 5/F-03 (same flat)"},
    {"number": "77330022", "label": "Wong Mei Ling · Azalea 5/F-04"},
    {"number": "66550033", "label": "Lee Chi Wai · Azalea 3/F-02"},
    {"number": "66550034", "label": "Lee Ka Yiu · Azalea 3/F-02 (same flat)"},
    {"number": "55660044", "label": "Cheung Ka Yan · Azalea 6/F-01"},
    {"number": "44770055", "label": "Ng Ho Yin · Azalea 2/F-07"},
    {"number": "33880066", "label": "Lam Siu Fong · Azalea 4/F-05"},
    {"number": "00009999", "label": "Unknown / visitor card"},
]

# paho-mqtt 2.x requires an explicit callback API version; we use no callbacks, so v2 is fine.
client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id="estate-simulator")
_ambient = {"on": False}


def publish(topic: str, payload: dict):
    client.publish(topic, json.dumps(payload), qos=1)


def ambient_loop():
    """Low-rate background traffic so the board looks alive: normal heartbeats + entrance taps."""
    i = 0
    while True:
        if _ambient["on"]:
            s = FIRE_SENSORS[i % len(FIRE_SENSORS)]
            publish(f"estate/sensor/{s['code']}", {"state": "NORMAL", "severity": "INFO"})
            c = CARDS[i % (len(CARDS) - 1)]           # skip the unknown card for ambient
            ent = ENTRANCES[i % len(ENTRANCES)]
            publish(f"estate/access/{ent['code']}", {"cardNumber": c["number"], "direction": "IN"})
            i += 1
        time.sleep(4)


@asynccontextmanager
async def lifespan(_: FastAPI):
    client.connect(BROKER_HOST, BROKER_PORT, keepalive=30)
    client.loop_start()
    threading.Thread(target=ambient_loop, daemon=True).start()
    yield
    client.loop_stop()
    client.disconnect()


app = FastAPI(title="Smart Estate device simulator", lifespan=lifespan)


class SensorMsg(BaseModel):
    code: str
    state: str = "ALERT"
    severity: str = "CRITICAL"


class AccessMsg(BaseModel):
    accessPoint: str
    cardNumber: str
    direction: str = "IN"


@app.post("/publish/sensor")
def publish_sensor(m: SensorMsg):
    publish(f"estate/sensor/{m.code}", {"state": m.state, "severity": m.severity})
    return {"published": f"estate/sensor/{m.code}", "state": m.state}


@app.post("/publish/access")
def publish_access(m: AccessMsg):
    publish(f"estate/access/{m.accessPoint}", {"cardNumber": m.cardNumber, "direction": m.direction})
    return {"published": f"estate/access/{m.accessPoint}", "card": m.cardNumber}


@app.post("/ambient/{state}")
def ambient(state: str):
    _ambient["on"] = (state == "on")
    return {"ambient": _ambient["on"]}


@app.get("/", response_class=HTMLResponse)
def panel():
    return PANEL_HTML


PANEL_HTML = """
<!doctype html><html><head><meta charset="utf-8"><title>Estate device simulator</title>
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>
  :root { color-scheme: light dark; }
  body { font-family: system-ui, sans-serif; max-width: 760px; margin: 24px auto; padding: 0 16px;
         background: #16110d; color: #f2e9df; }
  h1 { font-size: 1.3rem; } h2 { font-size: .9rem; text-transform: uppercase; letter-spacing: .08em;
       color: #d99a63; margin-top: 26px; }
  .row { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin: 7px 0; }
  .dev { flex: 1 1 260px; color: #c4b4a4; font-size: .9rem; }
  code { color: #9a8b7d; }
  button { border: 1px solid #3a2f26; background: #241c17; color: #f2e9df; padding: 7px 12px;
           border-radius: 8px; cursor: pointer; font-size: .85rem; }
  button:hover { background: #2c231c; }
  .alert { background: #4a1d1d; border-color: #7a2e2e; color: #ffd7d7; }
  .warn  { background: #4a3a17; border-color: #7a5f2e; color: #ffe9c2; }
  .ok { background: #1e3320; border-color: #2f5a34; color: #d5f2d5; }
  select, input { background: #241c17; color: #f2e9df; border: 1px solid #3a2f26; border-radius: 7px; padding: 6px; }
  #log { margin-top: 18px; font-family: ui-monospace, monospace; font-size: .8rem; color: #9a8b7d;
         white-space: pre-wrap; max-height: 160px; overflow: auto; }
  .toggle { background: #2c231c; }
</style></head><body>
<h1>🏢 Estate device simulator</h1>
<p style="color:#9a8b7d">Publishes to MQTT (localhost:1883). The Smart Estate backend ingests and updates its live dashboards.</p>

<h2>Fire sensors (smoke)</h2><div id="fire"></div>
<h2>Fire doors — must stay closed</h2><div id="firedoors"></div>
<h2>Normal doors</h2><div id="doors"></div>
<h2>Water-leak sensors</h2><div id="water"></div>

<h2>Octopus card readers</h2>
<div class="row">
  <select id="card"></select>
  <select id="reader"></select>
  <button onclick="tap('IN')">Tap · Entry</button>
  <button onclick="tap('OUT')">Tap · Exit</button>
</div>

<h2>Ambient traffic</h2>
<div class="row">
  <button class="toggle" id="amb" onclick="toggleAmbient()">Start background traffic</button>
  <span class="dev">Periodic normal heartbeats + entrance taps across towers.</span>
</div>

<div id="log"></div>

<script>
  const FIRE=%%FIRE%%, FIREDOORS=%%FIREDOORS%%, DOORS=%%DOORS%%, WATER=%%WATER%%, READERS=%%READERS%%, CARDS=%%CARDS%%;
  const log = (m) => { const el=document.getElementById('log'); el.textContent='› '+m+'\\n'+el.textContent; };
  async function post(url, body){ const r=await fetch(url,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)}); log(JSON.stringify(await r.json())); }

  function twoBtn(dev, aTxt, aCls, aBody, bTxt, bCls, bBody) {
    const div=document.createElement('div'); div.className='row';
    div.innerHTML=`<span class="dev">${dev.label} <code>${dev.code}</code></span>`;
    const a=document.createElement('button'); a.className=aCls; a.textContent=aTxt; a.onclick=()=>post('/publish/sensor',{code:dev.code,...aBody});
    const b=document.createElement('button'); b.className=bCls; b.textContent=bTxt; b.onclick=()=>post('/publish/sensor',{code:dev.code,...bBody});
    div.append(a,b); return div;
  }
  FIRE.forEach(d => document.getElementById('fire').append(
    twoBtn(d, 'Trip alert','alert',{state:'ALERT',severity:'CRITICAL'}, 'Clear','ok',{state:'NORMAL',severity:'INFO'})));
  FIREDOORS.forEach(d => document.getElementById('firedoors').append(
    twoBtn(d, 'Open (warning)','warn',{state:'OPEN',severity:'WARNING'}, 'Close','ok',{state:'CLOSED',severity:'INFO'})));
  DOORS.forEach(d => document.getElementById('doors').append(
    twoBtn(d, 'Open','warn',{state:'OPEN',severity:'INFO'}, 'Close','ok',{state:'CLOSED',severity:'INFO'})));
  WATER.forEach(d => document.getElementById('water').append(
    twoBtn(d, 'Trip alert','alert',{state:'ALERT',severity:'WARNING'}, 'Clear','ok',{state:'NORMAL',severity:'INFO'})));

  CARDS.forEach(c=>{const o=document.createElement('option');o.value=c.number;o.textContent=c.label+' ('+c.number+')';document.getElementById('card').append(o);});
  READERS.forEach(r=>{const o=document.createElement('option');o.value=r.code;o.textContent=r.label;document.getElementById('reader').append(o);});
  function tap(dir){ post('/publish/access',{accessPoint:reader.value,cardNumber:card.value,direction:dir}); }

  let ambient=false;
  async function toggleAmbient(){ ambient=!ambient; await fetch('/ambient/'+(ambient?'on':'off'),{method:'POST'});
    document.getElementById('amb').textContent = ambient?'Stop background traffic':'Start background traffic'; }
</script></body></html>
""".replace("%%FIRE%%", json.dumps(FIRE_SENSORS)) \
   .replace("%%FIREDOORS%%", json.dumps(FIRE_DOORS)) \
   .replace("%%DOORS%%", json.dumps(NORMAL_DOORS)) \
   .replace("%%WATER%%", json.dumps(WATER_SENSORS)) \
   .replace("%%READERS%%", json.dumps(READERS)) \
   .replace("%%CARDS%%", json.dumps(CARDS))
