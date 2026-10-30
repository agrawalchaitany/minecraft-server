<!-- language-switcher -->
[English](../troubleshooting.md) · **हिन्दी** · [Español](../es/troubleshooting.md) · [简体中文](../zh-CN/troubleshooting.md) · [Português (BR)](../pt-BR/troubleshooting.md) · [日本語](../ja/troubleshooting.md) · [한국어](../ko/troubleshooting.md)

# <img src="../assets/icons/troubleshooting.svg" width="28" height="28" alt=""> समस्या निवारण

यहाँ से शुरू करें:

```bash
make status          # is it running and healthy?
make logs            # what did it say?
```

ज़्यादातर समस्याओं में लॉग में एक साफ़ `ERROR:` लाइन होती है, जो सेटिंग या कारण का नाम बताती है। नीचे दिए गए संदेश वैसे ही अंग्रेज़ी में हैं जैसे लॉग में दिखते हैं।

## सर्वर शुरू नहीं हो रहा

**"You must accept the Minecraft EULA"**
`.env` में `EULA=TRUE` सेट करें, फिर `make up`।

**"Set RCON_PASSWORD in .env to your own random value"** या **"RCON_PASSWORD must not contain backslashes…"**
`openssl rand -hex 24` चलाएँ और नतीजा `.env` में डालें।

**"MEMORY … does not fit in the container limit"**
`CONTAINER_MEMORY` को संदेश में सुझाए गए मान तक बढ़ाएँ, या `MEMORY` घटाएँ। macOS पर **Settings → Resources** में Docker Desktop की मेमोरी भी जाँचें।

**"This world was last run on Minecraft X, but the server is now Y"**
आपने `MC_VERSION` घटा दिया है। उसे वापस करें, या पुराने संस्करण पर बना बैकअप रिस्टोर करें। देखें [Minecraft संस्करण](minecraft-versions.md#डाउनग्रेड-करना)।

**"Player data migration failed; not starting…"**
आमतौर पर ऑनलाइन मोड में बदलते समय Mojang का API नहीं मिल पाया। सर्वर का इंटरनेट कनेक्शन जाँचें; Docker अपने आप फिर से कोशिश करता है। देखें [ऑनलाइन और ऑफ़लाइन मोड](offline-mode.md#मोड-बदलने-पर-खिलाड़ी-डेटा)।

**exit code 137 के साथ बार-बार रीस्टार्ट**
कंटेनर अपनी मेमोरी सीमा तक पहुँच गया। `CONTAINER_MEMORY` बढ़ाएँ (कम से कम `MEMORY` + 1G), या `MEMORY` घटाएँ।

**"port is already allocated"**
होस्ट पर कोई और चीज़ पोर्ट 25565 इस्तेमाल कर रही है। उसे रोकें, या `SERVER_PORT=25566` सेट करें और उस पोर्ट से जुड़ें।

## बिल्ड की समस्याएँ

**"Minecraft X needs Java N but the image is being built with Java M"**
आपने सीधे `docker compose` से बिल्ड किया। `make up` इस्तेमाल करें, जो Java अपने आप चुनता है। अगर आपने खुद `JAVA_VERSION` सेट किया है, तो उसे हटा दें।

**"Minecraft X is not available on Paper" / "is too old"**
`make versions` से कोई संस्करण चुनें (1.17 या नया)।

**"Patching Paper failed (attempt N), retrying"**
बिल्ड के दौरान Mojang से डाउनलोड विफल हुआ; यह अपने आप फिर से कोशिश करता है। अगर सभी कोशिशें विफल हों, तो इंटरनेट कनेक्शन जाँचें और फिर से `make up` चलाएँ।

## खिलाड़ी जुड़ नहीं पा रहे

1. `make status` में `(healthy)` दिखना चाहिए।
2. सर्वर उसी मशीन पर है: `localhost` से जुड़ें।
3. क्लाउड सर्वर: होस्ट फ़ायरवॉल में TCP 25565 खोलें (`sudo ufw allow 25565/tcp`) **और** प्रोवाइडर के फ़ायरवॉल या security group में भी।
4. घरेलू नेटवर्क: राउटर पर TCP 25565 को होस्ट पर फ़ॉरवर्ड करें।
5. बिना आधिकारिक अकाउंट वाले खिलाड़ी: `ONLINE_MODE=false` सेट करें। देखें [ऑनलाइन और ऑफ़लाइन मोड](offline-mode.md)।
6. ऑफ़लाइन मोड में "Invalid username" या "already taken": देखें [यूज़रनेम नियम](offline-mode.md#यूज़रनेम-नियम)।

## Unhealthy

पहली बार शुरू होने पर दुनिया बनने में कुछ मिनट लग सकते हैं; health check पाँच मिनट देता है। अगर यह unhealthy ही रहे, तो `make logs` में errors देखें। अटके हुए सर्वर को [watchdog](reliability.md#अटकने-का-watchdog) अपने आप फिर से शुरू कर देता है।

## प्लगइन्स

**कोई प्लगइन लोड नहीं हुआ**
`make logs` में `[plugins] WARNING` लाइनें देखें। वे कारण बताती हैं: कोई संगत संस्करण नहीं, dependency गायब, नए Java की ज़रूरत, आदि। `make plugins` दिखाता है कि क्या इंस्टॉल है।

**"needs Java N (this server runs Java M)"**
उस प्लगइन का कोई भी संस्करण आपके Minecraft संस्करण और उसके Java, दोनों को सपोर्ट नहीं करता। नया `MC_VERSION` इस्तेमाल करें, या अगर आपका Minecraft संस्करण उस पर चलता है तो `JAVA_VERSION=N` सेट करें।

**"requires the plugin X, which could not be found automatically"**
X को Modrinth या Hangar पर ढूँढें और `plugins.txt` में जोड़ें, या उसे [हाथ से](plugins.md#दूसरी-साइटों-के-प्लगइन्स) इंस्टॉल करें।

**"another plugin install/update is already running"**
कोई तय अपडेट चल रहा है। एक मिनट बाद फिर कोशिश करें।

## बैकअप

**"not enough disk space"**
होस्ट पर जगह खाली करें, या `BACKUP_KEEP` घटाएँ। बैकअप डिस्क भरने से मना करते हैं, क्योंकि भरी डिस्क सर्वर को दुनिया सेव करने से रोक सकती है।

**"another backup has been running for 15 minutes"**
पिछला बैकअप अटका हुआ है, आमतौर पर बहुत धीमी डिस्क के कारण। `docker compose logs backup` देखें।

**`./backups` permission denied (Linux)**
`.env` में `PUID` और `PGID` को `id -u` / `id -g` पर सेट करें, फिर `make up`।

## सेटिंग्स लागू होती नहीं दिखतीं

`make restart` पुरानी सेटिंग्स रखता है; `.env` बदलने के बाद `make up` चलाएँ। `.env` की गेमप्ले सेटिंग्स हर शुरुआत पर `server.properties` को फिर से लिखती हैं, इसलिए अगर कोई सेटिंग `server.properties` में संभालनी है तो उसे `.env` में खाली छोड़ दें।
