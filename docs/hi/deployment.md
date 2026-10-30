<!-- language-switcher -->
[English](../deployment.md) · **हिन्दी** · [Español](../es/deployment.md) · [简体中文](../zh-CN/deployment.md) · [Português (BR)](../pt-BR/deployment.md) · [日本語](../ja/deployment.md) · [한국어](../ko/deployment.md)

# <img src="../assets/icons/deployment.svg" width="28" height="28" alt=""> डिप्लॉयमेंट

यही प्रोजेक्ट macOS पर (परीक्षण या दोस्तों के साथ खेलने के लिए) और Linux सर्वरों पर (हमेशा चालू रहने के लिए) चलता है। इमेज Intel/AMD (`amd64`) और ARM (`arm64`, Apple Silicon सहित) दोनों के लिए सीधे बनती है।

## Linux सर्वर (VPS, घरेलू सर्वर)

### 1. Docker इंस्टॉल करें

अपने डिस्ट्रीब्यूशन के लिए Docker के निर्देशों के अनुसार Docker Engine और Compose plugin इंस्टॉल करें, फिर:

```bash
sudo systemctl enable --now docker      # start Docker on boot
sudo usermod -aG docker "$USER"          # run docker without sudo (log out and in again)
```

### 2. प्रोजेक्ट कॉपी करें

```bash
git clone <your-repository> minecraft && cd minecraft
# or: scp -r Minecraft-Server/ user@server:~/minecraft
```

### 3. कॉन्फ़िगर करें

```bash
cp .env.example .env
id -u && id -g          # put these into PUID / PGID
```

`.env` में कम से कम `EULA`, `RCON_PASSWORD`, `MC_VERSION` (सटीक संस्करण तय करें), `MEMORY` और `CONTAINER_MEMORY` सेट करें।

| सर्वर RAM | `MEMORY` | `CONTAINER_MEMORY` |
|---|---|---|
| 4 GB | `2G` | `3G` |
| 8 GB | `5G` | `6G` |
| 16 GB | `10G` | `12G` |

ऑपरेटिंग सिस्टम के लिए लगभग 1 GB छोड़ें।

### 4. गेम पोर्ट खोलें

```bash
sudo ufw allow 25565/tcp
```

अगर आपके क्लाउड प्रोवाइडर में फ़ायरवॉल या security group है, तो वहाँ भी TCP 25565 खोलें। **25575 कभी न खोलें** (RCON); यह केवल अंदरूनी है।

### 5. शुरू करें

```bash
make up
```

रीबूट के बाद कंटेनर अपने आप वापस चालू हो जाते हैं।

## macOS

[Docker Desktop](https://www.docker.com/products/docker-desktop/) इंस्टॉल करें, फिर [शुरुआत करें](getting-started.md) का पालन करें। ध्यान रखने वाली बातें:

- Docker Desktop की अपनी मेमोरी सीमा होती है (**Settings → Resources**)। यह `CONTAINER_MEMORY` से ज़्यादा होनी चाहिए।
- आपके घरेलू नेटवर्क के दोस्त आपके Mac के लोकल IP से जुड़ते हैं। बाहर के दोस्तों के लिए, अपने राउटर पर TCP 25565 को अपने Mac पर फ़ॉरवर्ड करें।
- सर्वर चलने के लिए Docker Desktop चालू होना चाहिए। रीस्टार्ट के बाद यह वापस आए, इसके लिए "Start Docker Desktop when you sign in" चालू करें।

## बाहर रखे बैकअप

`./backups` सर्वर वाली ही डिस्क पर रहता है। इसे तय समय पर कहीं और कॉपी करें, उदाहरण के लिए सर्वर पर cron से `rsync` के ज़रिए:

```bash
# crontab -e   (every day at 05:00)
0 5 * * * rsync -a --delete /home/me/minecraft/backups/ backup-host:/srv/minecraft-backups/
```

कोई भी टूल चलेगा (क्लाउड स्टोरेज के लिए `rclone`, `restic` आदि)। केवल पूरी हो चुकी `mc-*.tar.gz` फ़ाइलें कॉपी करें; छिपी हुई `.partial` फ़ाइलें चल रहे बैकअप हैं।

## दूसरी मशीन पर ले जाना

1. पुरानी मशीन पर `make backup`।
2. नई मशीन पर वही `.env` और `plugins/plugins.txt` के साथ प्रोजेक्ट सेट करें।
3. बैकअप को नए `./backups/` में कॉपी करें।
4. `make up`, फिर `make restore FILE=…`। देखें [बैकअप और रिस्टोर](backups.md#नई-मशीन-पर-रिस्टोर-करना)।
5. खिलाड़ियों (या अपने DNS नाम) को नए पते पर भेजें।

## प्रोजेक्ट को खुद अपडेट करना

प्रोजेक्ट फ़ाइलों को नए संस्करण से बदलें, लेकिन `.env`, `plugins/plugins.txt` और `backups/` रखें। फिर:

```bash
make up
```

आपकी दुनिया Docker वॉल्यूम में रहती है और फ़ाइलें बदलने से उस पर कोई असर नहीं पड़ता।

## सुरक्षा चेकलिस्ट

- जब तक ऑफ़लाइन मोड की ज़रूरत न हो, `ONLINE_MODE=true` रखें, और अगर ज़रूरत है तो [जोखिम](offline-mode.md#जोखिम-यूज़रनेम-की-पुष्टि-नहीं-होती) पढ़ें।
- लंबा, रैंडम `RCON_PASSWORD` इस्तेमाल करें। पोर्ट 25575 कभी पब्लिश न करें।
- `.env` को निजी रखें; इसमें RCON पासवर्ड होता है। यह git और Docker बिल्ड से बाहर रखा जाता है।
- होस्ट को अपडेट रखें, और पासवर्ड की जगह SSH keys इस्तेमाल करें।
- केवल ज़रूरत वाले लोगों को ऑपरेटर बनाएँ; `make cmd C="deop Name"` इसे हटाता है।
- `MC_VERSION` तय रखें और प्लगइन अपडेट लागू करने से पहले उनकी समीक्षा करें।
