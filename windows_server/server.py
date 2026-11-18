"""
Remote Laptop Bluetooth server for Windows 10/11.
Receives newline-delimited JSON over a Bluetooth incoming COM port.
"""
import argparse
import json
import sys
import time

try:
    import serial
    import pyautogui
except ImportError:
    print("Install dependensi: py -m pip install pyserial pyautogui")
    raise

pyautogui.FAILSAFE = True  # Move mouse to top-left to abort automation.
pyautogui.PAUSE = 0.01

ALLOWED_KEYS = {"enter", "backspace", "esc", "tab", "space", "up", "down", "left", "right"}

def handle(data):
    cmd = data.get("cmd")
    if cmd == "MOVE":
        dx = max(-250, min(250, int(data.get("dx", 0))))
        dy = max(-250, min(250, int(data.get("dy", 0))))
        pyautogui.moveRel(dx, dy, duration=0)
    elif cmd == "CLICK":
        button = data.get("button")
        if button in ("left", "right"):
            pyautogui.click(button=button)
    elif cmd == "SCROLL":
        amount = max(-20, min(20, int(data.get("amount", 0))))
        pyautogui.scroll(amount)
    elif cmd == "TYPE":
        text = str(data.get("text", ""))[:2000]
        pyautogui.write(text, interval=0.005)
    elif cmd == "KEY":
        key = data.get("key")
        if key in ALLOWED_KEYS:
            pyautogui.press(key)
    elif cmd == "MEDIA":
        actions = {"vol_up": "volumeup", "vol_down": "volumedown", "mute": "volumemute"}
        key = actions.get(data.get("action"))
        if key:
            pyautogui.press(key)
    elif cmd == "SYSTEM" and data.get("action") == "lock":
        import ctypes
        ctypes.windll.user32.LockWorkStation()
    else:
        print("Perintah diabaikan:", cmd)

def main():
    parser = argparse.ArgumentParser(description="Bluetooth remote control receiver")
    parser.add_argument("--port", required=True, help="Incoming Bluetooth COM port, contoh COM6")
    parser.add_argument("--baud", type=int, default=9600)
    args = parser.parse_args()
    print(f"Menunggu koneksi Bluetooth pada {args.port} ...")
    print("Pastikan HP sudah dipasangkan dan aplikasi Remote Laptop dibuka.")
    with serial.Serial(args.port, args.baud, timeout=1) as ser:
        buffer = b""
        while True:
            chunk = ser.read(1024)
            if not chunk:
                continue
            buffer += chunk
            while b"\n" in buffer:
                line, buffer = buffer.split(b"\n", 1)
                try:
                    payload = json.loads(line.decode("utf-8"))
                    handle(payload)
                except (ValueError, UnicodeDecodeError) as exc:
                    print("Data tidak valid:", exc)
                except Exception as exc:
                    print("Perintah gagal:", exc)

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nServer dihentikan.")
