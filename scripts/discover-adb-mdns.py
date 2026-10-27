#!/usr/bin/env python3
"""
discover-adb-mdns.py
Descubre dispositivos Android con depuración inalámbrica (mDNS Zeroconf) en la red local
utilizando la utilidad nativa de macOS `dns-sd`.
Salida por línea: <SERIAL>|<IP:PUERTO>|<NOMBRE_DISPOSITIVO>|<SERVICIO_MDNS>
"""

import re
import socket
import subprocess
import sys

def discover(timeout_seconds: float = 2.0):
    cmd = ["dns-sd", "-Z", "_adb-tls-connect._tcp", "local"]
    try:
        proc = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True
        )
        try:
            stdout, _ = proc.communicate(timeout=timeout_seconds)
        except subprocess.TimeoutExpired:
            proc.kill()
            stdout, _ = proc.communicate()
    except Exception:
        return []

    srv_pattern = re.compile(r"^(\S+)\s+SRV\s+\d+\s+\d+\s+(\d+)\s+(\S+)", re.MULTILINE)
    txt_pattern = re.compile(r"^(\S+)\s+TXT\s+(.*)$", re.MULTILINE)

    services = {}
    for match in srv_pattern.finditer(stdout):
        svc_name, port, host = match.groups()
        services[svc_name] = {"port": int(port), "host": host.rstrip(".")}

    for match in txt_pattern.finditer(stdout):
        svc_name, txt_data = match.groups()
        if svc_name in services:
            serial_m = re.search(r"\"serial=([^\"]+)\"", txt_data)
            name_m = re.search(r"\"name=([^\"]+)\"", txt_data)
            if serial_m:
                services[svc_name]["serial"] = serial_m.group(1)
            if name_m:
                services[svc_name]["name"] = name_m.group(1)

    devices = []
    for svc_name, data in services.items():
        host = data.get("host")
        port = data.get("port")
        serial = data.get("serial", "")
        if host and port:
            try:
                ip = socket.gethostbyname(host)
                devices.append({
                    "service": svc_name,
                    "serial": serial,
                    "ip": ip,
                    "port": str(port),
                    "endpoint": f"{ip}:{port}",
                    "name": data.get("name", "")
                })
            except Exception:
                pass
    return devices

if __name__ == "__main__":
    found = discover()
    for d in found:
        print(f"{d['serial']}|{d['endpoint']}|{d['name']}|{d['service']}")
