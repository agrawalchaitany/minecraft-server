<div align="center">

<img src="../assets/banner.svg" alt="Minecraft Server: Docker के लिए खुद बनाया गया, production-ready Paper सर्वर" width="100%">

[![Minecraft](https://img.shields.io/badge/Minecraft-1.17%20to%20latest-62B47A)](../hi/minecraft-versions.md)
[![Paper](https://img.shields.io/badge/Paper-stable%20builds-1f6feb)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-17%20%7C%2021%20%7C%2025%20(auto)-orange?logo=openjdk&logoColor=white)](../hi/minecraft-versions.md#java-कैसे-चुना-जाता-है)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![Platforms](https://img.shields.io/badge/platform-linux%2Famd64%20%7C%20linux%2Farm64%20%7C%20macOS-lightgrey)](../hi/deployment.md)

[जल्दी शुरुआत](#जल्दी-शुरुआत) ·
[सुविधाएँ](#सुविधाएँ) ·
[दस्तावेज़](../hi/README.md) ·
[समस्या निवारण](../hi/troubleshooting.md)

<!-- language-switcher -->
[English](../../README.md) · **हिन्दी** · [Español](README.es.md) · [简体中文](README.zh-CN.md) · [Português (BR)](README.pt-BR.md) · [日本語](README.ja.md) · [한국어](README.ko.md)

</div>

---

1.17 से लेकर नवीनतम तक, Minecraft: Java Edition का कोई भी संस्करण [Paper](https://papermc.io) पर एक ही कमांड से चलाइए। सर्वर क्रैश होने पर खुद फिर से शुरू होता है, दुनिया (world) को सही तरीके से सेव करता है, तय समय पर बैकअप लेता है, प्लगइन्स को संगत (compatible) रखता है, और ऐसी सेटिंग्स को मना कर देता है जो उसे खराब कर सकती हैं।

सब कुछ शुरू से बनाया गया है: किसी तीसरे पक्ष की सर्वर इमेज या स्क्रिप्ट का उपयोग नहीं होता। केवल आधिकारिक `eclipse-temurin` Java इमेज और आधिकारिक PaperMC, Mojang, Modrinth और Hangar डाउनलोड सेवाओं का उपयोग होता है। हर सर्वर और प्लगइन jar का checksum जाँचा जाता है।

## जल्दी शुरुआत

### तैयार इमेज

कुछ बनाने की ज़रूरत नहीं: Java 25 पर Minecraft 26.3 (नवीनतम stable), बिना प्लगइन्स, Intel/AMD और ARM के लिए।

```bash
docker run -d --name minecraft --restart unless-stopped \
  -p 25565:25565 -e EULA=TRUE -e MEMORY=2G \
  -v minecraft-data:/data \
  agrawalchaitany/minecraft-server:26.3
```

`EULA=TRUE` सेट करने का मतलब है कि आप [Minecraft EULA](https://aka.ms/MinecraftEULA) स्वीकार करते हैं। `localhost:25565` से जुड़ें; कंसोल `docker exec -it minecraft rcon-cli` से खोलें। यही इमेज GitHub Packages पर `ghcr.io/agrawalchaitany/minecraft-server` के रूप में भी है।

तय समय पर बैकअप (`./backups` में) के साथ चलाने के लिए तैयार [compose फ़ाइल](../../quickstart/compose.yaml) इस्तेमाल करें:

```bash
mkdir minecraft && cd minecraft
curl -fsSLO https://raw.githubusercontent.com/agrawalchaitany/minecraft-server/main/quickstart/compose.yaml
echo EULA=TRUE > .env
docker compose up -d
```

### सोर्स से बनाएँ

Minecraft संस्करण चुनने, प्लगइन्स जोड़ने या कुछ और बदलने के लिए, इसे खुद बनाएँ:

```bash
cp .env.example .env
```

`.env` में बदलाव करें:

```env
EULA=TRUE                         # accept https://aka.ms/MinecraftEULA
RCON_PASSWORD=                    # optional; generated automatically if empty
MC_VERSION=latest                 # or an exact version, e.g. 1.21.4 (see: make versions)
```

शुरू करें:

```bash
make up
make status        # wait for (healthy), then connect to localhost:25565
```

पूरी गाइड: **[शुरुआत करें](../hi/getting-started.md)**।

## सुविधाएँ

<img src="../assets/icons/versions.svg" width="36" height="36" align="left" hspace="10" alt="">

**कोई भी संस्करण**<br>
Minecraft 1.17 से नवीनतम तक। सही Java (17, 21 या 25) अपने आप चुना जाता है। अपग्रेड से पहले दुनिया का बैकअप लिया जाता है; जो डाउनग्रेड दुनिया को खराब कर सकते हैं, उन्हें मना कर दिया जाता है। [और जानें](../hi/minecraft-versions.md)

<img src="../assets/icons/reliability.svg" width="36" height="36" align="left" hspace="10" alt="">

**खुद ठीक होना**<br>
क्रैश और मेमोरी खत्म होने के बाद अपने आप फिर से शुरू। एक health check, और एक watchdog जो अटके हुए सर्वर को फिर से शुरू करता है। [और जानें](../hi/reliability.md)

<img src="../assets/icons/shutdown.svg" width="36" height="36" align="left" hspace="10" alt="">

**सुरक्षित बंद होना**<br>
हर बार रोकने से पहले सर्वर कंसोल के ज़रिए सभी दुनियाएँ सेव की जाती हैं। [और जानें](../hi/reliability.md#सुरक्षित-बंद-होना)

<img src="../assets/icons/backups.svg" width="36" height="36" align="left" hspace="10" alt="">

**बैकअप**<br>
तय समय पर, खिलाड़ियों के ऑनलाइन रहते हुए भी सुसंगत; लॉक किए जाते हैं ताकि दो कभी एक साथ न चलें; जाँचे जाते हैं, डिस्क स्पेस देखा जाता है और पुराने हटाए जाते हैं। अपग्रेड, माइग्रेशन, प्लगइन अपडेट और रिस्टोर से पहले अपने आप लिए जाते हैं। [और जानें](../hi/backups.md)

<img src="../assets/icons/restore.svg" width="36" height="36" align="left" hspace="10" alt="">

**सुरक्षित रिस्टोर**<br>
एक कमांड। पहले फ़ाइल जाँचता है, सुरक्षा बैकअप लेता है, और एक staging फ़ोल्डर में निकालता है ताकि कोई गड़बड़ी मौजूदा दुनिया को न मिटा सके। [और जानें](../hi/backups.md#रिस्टोर-करना)

<img src="../assets/icons/plugins.svg" width="36" height="36" align="left" hspace="10" alt="">

**प्लगइन मैनेजर**<br>
`plugins.txt` में प्लगइन्स लिखें। हर एक के लिए, jar की खुद जाँच करके वह नवीनतम संस्करण चुना जाता है जो आपके Minecraft और Java पर सच में चलता है। ज़रूरी dependencies इंस्टॉल की जाती हैं। टकराव और डुप्लिकेट पकड़े जाते हैं। ज़रूरत पर या रोज़ अपडेट। [और जानें](../hi/plugins.md)

<img src="../assets/icons/players.svg" width="36" height="36" align="left" hspace="10" alt="">

**ऑफ़लाइन मोड**<br>
बिना आधिकारिक अकाउंट वाले खिलाड़ियों को जुड़ने दें, Mojang के यूज़रनेम नियम लागू रहते हैं और मोड बदलने पर खिलाड़ियों का सामान सुरक्षित रहता है। [और जानें](../hi/offline-mode.md)

<img src="../assets/icons/guard.svg" width="36" height="36" align="left" hspace="10" alt="">

**सुरक्षा जाँचें**<br>
शुरू होते समय हर सेटिंग साफ़ संदेश के साथ जाँची जाती है। टाइपो से क्रैश-लूप नहीं होते। [और जानें](../hi/reliability.md#सेटिंग्स-की-जाँच)

<img src="../assets/icons/security.svg" width="36" height="36" align="left" hspace="10" alt="">

**न्यूनतम अधिकार**<br>
non-root यूज़र के रूप में चलता है। रिमोट कंसोल Docker के बाहर कभी उजागर नहीं होता। लॉग rotate किए जाते हैं।

## रोज़मर्रा की कमांड

| कमांड | |
|---|---|
| `make up` | शुरू करें, या `.env` के बदलाव लागू करें |
| `make down` | रोकें (दुनियाएँ सेव होती हैं) |
| `make status` / `make logs` | स्थिति और लॉग |
| `make console` / `make cmd C="op Steve"` | सर्वर कंसोल |
| `make backup` / `make restore FILE=…` | बैकअप और रिस्टोर |
| `make update-plugins` / `make plugins` | `plugins.txt` लागू करें और इंस्टॉल प्लगइन्स देखें |
| `make versions` / `make update` | Minecraft संस्करणों की सूची; Paper अपडेट करें |

सभी कमांड: [संचालन](../hi/operations.md)।

## दस्तावेज़

| | गाइड | |
|:-:|---|---|
| <img src="../assets/icons/start.svg" width="20" height="20" alt=""> | [शुरुआत करें](../hi/getting-started.md) | पाँच मिनट में चालू |
| <img src="../assets/icons/architecture.svg" width="20" height="20" alt=""> | [आर्किटेक्चर](../hi/architecture.md) | `make up` से चलते सर्वर तक, यह कैसे काम करता है |
| <img src="../assets/icons/configuration.svg" width="20" height="20" alt=""> | [कॉन्फ़िगरेशन](../hi/configuration.md) | `.env` की हर सेटिंग |
| <img src="../assets/icons/operations.svg" width="20" height="20" alt=""> | [संचालन](../hi/operations.md) | रोज़ के काम |
| <img src="../assets/icons/versions.svg" width="20" height="20" alt=""> | [Minecraft संस्करण](../hi/minecraft-versions.md) | संस्करण, Java, अपग्रेड और डाउनग्रेड |
| <img src="../assets/icons/plugins.svg" width="20" height="20" alt=""> | [प्लगइन्स](../hi/plugins.md) | `plugins.txt`, संगतता की जाँच, ऑटो-अपडेट, मॉड |
| <img src="../assets/icons/backups.svg" width="20" height="20" alt=""> | [बैकअप और रिस्टोर](../hi/backups.md) | बैकअप कैसे सुसंगत और रिस्टोर कैसे सुरक्षित रहते हैं |
| <img src="../assets/icons/reliability.svg" width="20" height="20" alt=""> | [विश्वसनीयता](../hi/reliability.md) | Health check, watchdog, बंद होना, मेमोरी, जाँचें |
| <img src="../assets/icons/players.svg" width="20" height="20" alt=""> | [ऑनलाइन और ऑफ़लाइन मोड](../hi/offline-mode.md) | बिना अकाउंट वाले खिलाड़ी, यूज़रनेम नियम, खिलाड़ी डेटा |
| <img src="../assets/icons/deployment.svg" width="20" height="20" alt=""> | [डिप्लॉयमेंट](../hi/deployment.md) | Linux सर्वर, macOS, फ़ायरवॉल, बाहर रखे बैकअप |
| <img src="../assets/icons/edge-cases.svg" width="20" height="20" alt=""> | [असामान्य स्थितियाँ](../hi/edge-cases.md) | असामान्य स्थितियाँ और उन्हें कैसे संभाला जाता है |
| <img src="../assets/icons/troubleshooting.svg" width="20" height="20" alt=""> | [समस्या निवारण](../hi/troubleshooting.md) | लक्षण और समाधान |

## ज़रूरतें

- Docker Desktop (macOS/Windows) या Compose plugin के साथ Docker Engine 24+ (Linux)
- `make`
- डिफ़ॉल्ट सेटिंग्स के लिए लगभग 3 GB खाली मेमोरी

## प्रोजेक्ट की संरचना

```
Dockerfile, compose.yaml, Makefile, .env.example
scripts/     startup, backup, restore, and helper scripts
tools/       dependency-free Java tools (RCON, versions, plugins, player migration)
plugins/     plugins.txt and the built-in username-rules plugin
docs/        documentation
```

## अस्वीकरण

Mojang Studios, Microsoft या PaperMC से संबद्ध नहीं। Minecraft, Mojang Studios का ट्रेडमार्क है। सर्वर चलाने के लिए [Minecraft EULA](https://aka.ms/MinecraftEULA) स्वीकार करना ज़रूरी है।
