<!-- language-switcher -->
[English](../operations.md) · **हिन्दी** · [Español](../es/operations.md) · [简体中文](../zh-CN/operations.md) · [Português (BR)](../pt-BR/operations.md) · [日本語](../ja/operations.md) · [한국어](../ko/operations.md)

# <img src="../assets/icons/operations.svg" width="28" height="28" alt=""> संचालन

रोज़ के काम। सभी कमांड प्रोजेक्ट फ़ोल्डर से चलाएँ।

## कमांड संदर्भ

| कमांड | यह क्या करता है |
|---|---|
| `make up` | ज़रूरत हो तो बिल्ड करें, `.env` लागू करें और सब कुछ शुरू करें |
| `make down` | सब कुछ सुरक्षित रूप से रोकें (दुनियाएँ सेव होती हैं) |
| `make restart` | गेम सर्वर को सुरक्षित रूप से फिर से शुरू करें (मौजूदा सेटिंग्स बनी रहती हैं) |
| `make status` | कंटेनर की स्थिति और health |
| `make logs` | लॉग देखते रहें (<kbd>Ctrl</kbd>+<kbd>C</kbd> केवल देखना बंद करता है, सर्वर नहीं) |
| `make console` | इंटरैक्टिव सर्वर कंसोल (बाहर निकलने के लिए `exit` लिखें) |
| `make cmd C="…"` | सर्वर की एक कमांड चलाएँ |
| `make backup` | अभी बैकअप लें |
| `make backups` | बैकअप की सूची |
| `make restore FILE=…` | बैकअप रिस्टोर करें; देखें [बैकअप और रिस्टोर](backups.md) |
| `make update` | आपके Minecraft संस्करण के नवीनतम Paper बिल्ड के साथ फिर से बिल्ड करें |
| `make versions` | Minecraft संस्करणों और हर एक के लिए ज़रूरी Java की सूची |
| `make plugins` | इंस्टॉल प्लगइन्स और हर एक के होने का कारण दिखाएँ |
| `make update-plugins` | `plugins/plugins.txt` लागू करें; देखें [प्लगइन्स](plugins.md) |

## क्या यह healthy है?

```bash
make status
```

`(healthy)` का मतलब है कि सर्वर जवाब दे रहा है। पहली बार शुरू होने पर नई दुनिया बनने में कुछ समय लग सकता है; health check पाँच मिनट तक इंतज़ार करता है। "healthy" का मतलब जानने के लिए देखें [विश्वसनीयता](reliability.md#health-check)।

## सर्वर कंसोल

```bash
make console            # interactive
make cmd C="list"       # one command
make cmd C="op Steve"
make cmd C="whitelist add Steve"
make cmd C="say Restarting soon"
```

आप `docker compose attach mc` से सीधे कच्चे कंसोल से भी जुड़ सकते हैं। अलग होने के लिए <kbd>Ctrl</kbd>+<kbd>P</kbd> और फिर <kbd>Ctrl</kbd>+<kbd>Q</kbd> दबाएँ। वहाँ <kbd>Ctrl</kbd>+<kbd>C</kbd> दबाने से सर्वर सुरक्षित रूप से रुक जाता है।

## लॉग

```bash
make logs                                  # all services, follow
docker compose logs mc --since 1h          # game server, last hour
docker compose logs backup                 # backup history
docker compose exec mc ls logs             # Paper's own log files
```

Docker हर कंटेनर के लिए अधिकतम 5 × 10 MB लॉग रखता है, इसलिए लॉग से डिस्क कभी नहीं भरती।

## सर्वर फ़ाइलें बदलना

सर्वर की फ़ाइलें `mc-data` वॉल्यूम में रहती हैं। किसी एक को बदलने के लिए:

```bash
docker compose cp mc:/data/config/paper-global.yml .
# edit it
docker compose cp paper-global.yml mc:/data/config/paper-global.yml
make restart
```

जो सेटिंग्स `.env` नियंत्रित करता है (देखें [कॉन्फ़िगरेशन संदर्भ](configuration.md#गेमप्ले)), वे हर शुरुआत पर फिर से लिखी जाती हैं। उन्हें `.env` में ही बदलें।

## अपडेट करना

| क्या | कैसे |
|---|---|
| नया Paper बिल्ड, वही Minecraft संस्करण | `make update` |
| नया Minecraft संस्करण | `MC_VERSION` बदलें, फिर `make up`। दुनिया अपग्रेड होने से पहले बैकअप लिया जाता है। देखें [Minecraft संस्करण](minecraft-versions.md)। |
| प्लगइन्स | `make update-plugins`। देखें [प्लगइन्स](plugins.md)। |
| यह प्रोजेक्ट | प्रोजेक्ट फ़ाइलें बदलें (`.env`, `plugins/plugins.txt` और `backups/` रखें), फिर `make up` |

## डिस्क उपयोग

```bash
docker system df -v | grep mc-data     # world volume size
du -sh backups                         # backups
```

`BACKUP_KEEP` तय करता है कि बैकअप कितनी जगह लेंगे। अगर बैकअप से डिस्क भर सकती है तो वह चलने से मना कर देता है; देखें [बैकअप और रिस्टोर](backups.md#डिस्क-जगह-की-सुरक्षा)।

## फिर से शुरू से

दुनिया और सर्वर का सारा डेटा मिटाने के लिए (`./backups` के बैकअप बने रहते हैं):

```bash
make backup                 # optional: keep a final copy
docker compose down -v      # -v deletes the mc-data volume
make up
```
