package com.laralbir.japantraveltools.feature.emergency.data.catalog

import com.laralbir.japantraveltools.feature.emergency.domain.model.ConsularMissionItem
import com.laralbir.japantraveltools.feature.emergency.domain.model.EmergencyCategory
import com.laralbir.japantraveltools.feature.emergency.domain.model.EmergencyContactNumber
import com.laralbir.japantraveltools.feature.emergency.domain.model.EmergencyPhraseItem

object SeedEmergencyCatalog {

    val emergencyNumbers: List<EmergencyContactNumber> = listOf(
        EmergencyContactNumber(
            id = "num-police-110",
            nameEn = "Police",
            nameEs = "Policía",
            japaneseName = "警察 (110番)",
            phoneNumber = "110",
            descriptionEn = "Accidents, crimes, theft, traffic incidents",
            descriptionEs = "Accidentes, delitos, robos, incidentes de tráfico",
            iconName = "local_police",
            isPrimaryEmergency = true
        ),
        EmergencyContactNumber(
            id = "num-fire-ambulance-119",
            nameEn = "Fire & Ambulance",
            nameEs = "Bomberos y Ambulancias",
            japaneseName = "火事・救急 (119番)",
            phoneNumber = "119",
            descriptionEn = "Fires, medical emergencies, life-threatening rescue",
            descriptionEs = "Incendios, urgencias médicas vitales, rescate",
            iconName = "medical_services",
            isPrimaryEmergency = true
        ),
        EmergencyContactNumber(
            id = "num-coast-guard-118",
            nameEn = "Japan Coast Guard",
            nameEs = "Guardia Costera de Japón",
            japaneseName = "海上保安庁 (118番)",
            phoneNumber = "118",
            descriptionEn = "Maritime emergencies, boat accidents, coastal distress",
            descriptionEs = "Emergencias marítimas, accidentes en ferry, incidentes costeros",
            iconName = "sailing",
            isPrimaryEmergency = true
        ),
        EmergencyContactNumber(
            id = "num-jnto-hotline",
            nameEn = "Japan Visitor Hotline (JNTO)",
            nameEs = "Línea de Asistencia al Turista (JNTO)",
            japaneseName = "ジャパン・ビジター・ホットライン",
            phoneNumber = "0570-000-911",
            descriptionEn = "24/7 tourist support, multilingual help (EN, ES, ZH, KO)",
            descriptionEs = "Asistencia al viajero 24/7 y soporte médico multilingüe en español",
            iconName = "support_agent",
            isPrimaryEmergency = true
        ),
        EmergencyContactNumber(
            id = "num-tokyo-police-english",
            nameEn = "Tokyo Metropolitan Police (English)",
            nameEs = "Policía Metropolitana de Tokio (Inglés)",
            japaneseName = "警視庁英語相談ダイヤル",
            phoneNumber = "03-3501-0110",
            descriptionEn = "General counseling, non-urgent assistance in English",
            descriptionEs = "Consultas no urgentes y asistencia general en inglés",
            iconName = "hearing",
            isPrimaryEmergency = false
        ),
        EmergencyContactNumber(
            id = "num-emergency-consultation-7119",
            nameEn = "Emergency Medical Consultation",
            nameEs = "Consulta Médica Rápida de Urgencias",
            japaneseName = "救急安心センター事業 (#7119)",
            phoneNumber = "#7119",
            descriptionEn = "Advice on whether to go to the hospital or call an ambulance",
            descriptionEs = "Orientación sobre si acudir a urgencias o llamar a una ambulancia",
            iconName = "health_and_safety",
            isPrimaryEmergency = false
        )
    )

    val consularMissions: List<ConsularMissionItem> = listOf(
        // España - Embajada y Consulados Honorarios
        ConsularMissionItem(
            id = "es-embassy-tokyo",
            countryCode = "ES",
            countryName = "España",
            city = "Tokio",
            addressJapanese = "〒106-0032 東京都港区六本木1-3-29",
            addressRomaji = "1-3-29 Roppongi, Minato-ku, Tokyo 106-0032",
            emergencyPhone24h = "+81 90 1055 1410",
            standardPhone = "+81 3 3583 8531",
            latitude = 35.666997,
            longitude = 139.741006,
            websiteUrl = "https://www.exteriores.gob.es/embajadas/tokio"
        ),
        ConsularMissionItem(
            id = "es-consulate-osaka",
            countryCode = "ES",
            countryName = "España",
            city = "Osaka",
            addressJapanese = "〒530-0004 大阪府大阪市北区堂島浜2-1-40 サントリービル内",
            addressRomaji = "Suntory Bldg, 2-1-40 Dojimahama, Kita-ku, Osaka 530-0004",
            emergencyPhone24h = "+81 90 1055 1410",
            standardPhone = "+81 6 6340 2521",
            latitude = 34.6953,
            longitude = 135.4952,
            websiteUrl = "https://www.exteriores.gob.es/embajadas/tokio"
        ),
        ConsularMissionItem(
            id = "es-consulate-takayama",
            countryCode = "ES",
            countryName = "España",
            city = "Takayama",
            addressJapanese = "〒506-8688 岐阜県高山市花岡町2-60 濃飛乗合自動車内",
            addressRomaji = "Nohi Bus Bldg, 2-60 Hanaoka-cho, Takayama, Gifu 506-8688",
            emergencyPhone24h = "+81 90 1055 1410",
            standardPhone = "+81 577 33 5500",
            latitude = 36.1432,
            longitude = 137.2519,
            websiteUrl = "https://www.exteriores.gob.es/embajadas/tokio"
        ),
        ConsularMissionItem(
            id = "es-consulate-fukuoka",
            countryCode = "ES",
            countryName = "España",
            city = "Fukuoka",
            addressJapanese = "〒810-8720 福岡県福岡市中央区天神1-7-11 西日本鉄道内",
            addressRomaji = "Nishitetsu Bldg, 1-7-11 Tenjin, Chuo-ku, Fukuoka 810-8720",
            emergencyPhone24h = "+81 90 1055 1410",
            standardPhone = "+81 92 711 5000",
            latitude = 33.5902,
            longitude = 130.4017,
            websiteUrl = "https://www.exteriores.gob.es/embajadas/tokio"
        ),
        ConsularMissionItem(
            id = "es-consulate-nagoya",
            countryCode = "ES",
            countryName = "España",
            city = "Nagoya",
            addressJapanese = "〒450-8501 愛知県名古屋市中村区名駅1-1-4 名古屋鉄道内",
            addressRomaji = "Meitetsu Bldg, 1-1-4 Meieki, Nakamura-ku, Nagoya, Aichi 450-8501",
            emergencyPhone24h = "+81 90 1055 1410",
            standardPhone = "+81 52 584 1000",
            latitude = 35.1709,
            longitude = 136.8815,
            websiteUrl = "https://www.exteriores.gob.es/embajadas/tokio"
        ),
        ConsularMissionItem(
            id = "es-consulate-sapporo",
            countryCode = "ES",
            countryName = "España",
            city = "Sapporo",
            addressJapanese = "〒060-0001 北海道札幌市中央区北1条西3-3 敷島ビル内",
            addressRomaji = "Shikishima Bldg, Kita 1 Jonishi 3-3, Chuo-ku, Sapporo, Hokkaido 060-0001",
            emergencyPhone24h = "+81 90 1055 1410",
            standardPhone = "+81 11 251 0111",
            latitude = 43.0639,
            longitude = 141.3533,
            websiteUrl = "https://www.exteriores.gob.es/embajadas/tokio"
        ),

        // México
        ConsularMissionItem(
            id = "mx-embassy-tokyo",
            countryCode = "MX",
            countryName = "México",
            city = "Tokio",
            addressJapanese = "〒100-0014 東京都千代田区永田町2-15-1",
            addressRomaji = "2-15-1 Nagata-cho, Chiyoda-ku, Tokyo 100-0014",
            emergencyPhone24h = "+81 80 3215 0340",
            standardPhone = "+81 3 3580 2961",
            latitude = 35.6766,
            longitude = 139.7397,
            websiteUrl = "https://embamex.sre.gob.mx/japon"
        ),

        // Colombia
        ConsularMissionItem(
            id = "co-embassy-tokyo",
            countryCode = "CO",
            countryName = "Colombia",
            city = "Tokio",
            addressJapanese = "〒141-0021 東京都品川区上大崎3-10-53",
            addressRomaji = "3-10-53 Kamiosaki, Shinagawa-ku, Tokyo 141-0021",
            emergencyPhone24h = "+81 90 2496 1568",
            standardPhone = "+81 3 3440 6451",
            latitude = 35.6339,
            longitude = 139.7196,
            websiteUrl = "https://japon.embajada.gov.co"
        ),

        // Argentina
        ConsularMissionItem(
            id = "ar-embassy-tokyo",
            countryCode = "AR",
            countryName = "Argentina",
            city = "Tokio",
            addressJapanese = "〒106-0046 東京都港区元麻布2-14-14",
            addressRomaji = "2-14-14 Motoazabu, Minato-ku, Tokyo 106-0046",
            emergencyPhone24h = "+81 90 4453 5032",
            standardPhone = "+81 3 5420 7101",
            latitude = 35.6534,
            longitude = 139.7291,
            websiteUrl = "https://ejapo.cancilleria.gob.ar"
        ),

        // Chile
        ConsularMissionItem(
            id = "cl-embassy-tokyo",
            countryCode = "CL",
            countryName = "Chile",
            city = "Tokio",
            addressJapanese = "〒105-0014 東京都港区芝3-1-14 芝エステートビル8F",
            addressRomaji = "Shiba Estate Bldg 8F, 3-1-14 Shiba, Minato-ku, Tokyo 105-0014",
            emergencyPhone24h = "+81 90 7711 7734",
            standardPhone = "+81 3 3452 7561",
            latitude = 35.6515,
            longitude = 139.7495,
            websiteUrl = "https://chile.gob.cl/japon"
        ),

        // Perú
        ConsularMissionItem(
            id = "pe-embassy-tokyo",
            countryCode = "PE",
            countryName = "Perú",
            city = "Tokio",
            addressJapanese = "〒141-0022 東京都品川区東五反田1-3-10 プラザ広尾ビル",
            addressRomaji = "Plaza Hiroo, 1-3-10 Higashigotanda, Shinagawa-ku, Tokyo 141-0022",
            emergencyPhone24h = "+81 90 2317 2661",
            standardPhone = "+81 3 3448 8558",
            latitude = 35.6262,
            longitude = 139.7235,
            websiteUrl = "https://www.gob.pe/embajada-del-peru-en-japon"
        )
    )

    val emergencyPhrases: List<EmergencyPhraseItem> = listOf(
        // MEDICAL
        EmergencyPhraseItem(
            id = "phrase-med-ambulance",
            category = EmergencyCategory.MEDICAL,
            spanishText = "¡Por favor, llame a una ambulancia!",
            englishText = "Please call an ambulance!",
            japaneseKanji = "救急車を呼んでください",
            romaji = "Kyūkyūsha o yonde kudasai"
        ),
        EmergencyPhraseItem(
            id = "phrase-med-chest-pain",
            category = EmergencyCategory.MEDICAL,
            spanishText = "Tengo un dolor muy fuerte en el pecho",
            englishText = "I have severe chest pain",
            japaneseKanji = "胸が激しく痛みます",
            romaji = "Mune ga hageshiku itamimasu"
        ),
        EmergencyPhraseItem(
            id = "phrase-med-breathing",
            category = EmergencyCategory.MEDICAL,
            spanishText = "Me cuesta respirar",
            englishText = "I am having trouble breathing",
            japaneseKanji = "息が苦しいです",
            romaji = "Iki ga kurushii desu"
        ),
        EmergencyPhraseItem(
            id = "phrase-med-diabetes",
            category = EmergencyCategory.MEDICAL,
            spanishText = "Soy diabético, necesito insulina",
            englishText = "I am diabetic, I need insulin",
            japaneseKanji = "私は糖尿病です。インスリンが必要です",
            romaji = "Watashi wa tōnyōbyō desu. Insurin ga hitsuyō desu"
        ),
        EmergencyPhraseItem(
            id = "phrase-med-doctor",
            category = EmergencyCategory.MEDICAL,
            spanishText = "Necesito un médico que hable inglés o español",
            englishText = "I need a doctor who speaks English or Spanish",
            japaneseKanji = "英語かスペイン語が話せる医師をお願いします",
            romaji = "Eigo ka Supeingo ga hanaseru ishi o onegai shimasu"
        ),
        EmergencyPhraseItem(
            id = "phrase-med-hospital",
            category = EmergencyCategory.MEDICAL,
            spanishText = "¿Dónde está el hospital de urgencias más cercano?",
            englishText = "Where is the nearest emergency hospital?",
            japaneseKanji = "一番近い救急病院はどこですか？",
            romaji = "Ichiban chikai kyūkyū byōin wa doko desu ka?"
        ),

        // POLICE
        EmergencyPhraseItem(
            id = "phrase-pol-call",
            category = EmergencyCategory.POLICE,
            spanishText = "¡Por favor, llame a la policía!",
            englishText = "Please call the police!",
            japaneseKanji = "警察を呼んでください",
            romaji = "Keisatsu o yonde kudasai"
        ),
        EmergencyPhraseItem(
            id = "phrase-pol-lost-items",
            category = EmergencyCategory.POLICE,
            spanishText = "He perdido mi pasaporte y mi cartera",
            englishText = "I have lost my passport and wallet",
            japaneseKanji = "パスポートと財布を失くしました",
            romaji = "Pasupōto to saifu o nakushimashita"
        ),
        EmergencyPhraseItem(
            id = "phrase-pol-koban",
            category = EmergencyCategory.POLICE,
            spanishText = "¿Dónde está el puesto de policía (kōban) más cercano?",
            englishText = "Where is the nearest police box (koban)?",
            japaneseKanji = "一番近い交番はどこですか？",
            romaji = "Ichiban chikai kōban wa doko desu ka?"
        ),
        EmergencyPhraseItem(
            id = "phrase-pol-stolen",
            category = EmergencyCategory.POLICE,
            spanishText = "Me han robado la mochila",
            englishText = "My backpack was stolen",
            japaneseKanji = "リュックサックを盗まれました",
            romaji = "Ryukkusakku o nusumaremashita"
        ),
        EmergencyPhraseItem(
            id = "phrase-pol-help",
            category = EmergencyCategory.POLICE,
            spanishText = "¡Socorro! ¡Ayuda!",
            englishText = "Help! Somebody help me!",
            japaneseKanji = "助けて！",
            romaji = "Tasukete!"
        ),

        // DISASTER
        EmergencyPhraseItem(
            id = "phrase-dis-shelter",
            category = EmergencyCategory.DISASTER,
            spanishText = "¿Dónde está el refugio de evacuación?",
            englishText = "Where is the evacuation shelter?",
            japaneseKanji = "避難所はどこですか？",
            romaji = "Hinanjo wa doko desu ka?"
        ),
        EmergencyPhraseItem(
            id = "phrase-dis-safe",
            category = EmergencyCategory.DISASTER,
            spanishText = "¿Es seguro quedarse aquí?",
            englishText = "Is it safe to stay here?",
            japaneseKanji = "ここにいても安全ですか？",
            romaji = "Koko ni itemo anzen desu ka?"
        ),
        EmergencyPhraseItem(
            id = "phrase-dis-tsunami",
            category = EmergencyCategory.DISASTER,
            spanishText = "¿Hay alerta de tsunami?",
            englishText = "Is there a tsunami warning?",
            japaneseKanji = "津波警報は出ていますか？",
            romaji = "Tsunami keihō wa dete imasu ka?"
        ),
        EmergencyPhraseItem(
            id = "phrase-dis-earthquake",
            category = EmergencyCategory.DISASTER,
            spanishText = "¿Qué debemos hacer en caso de terremoto?",
            englishText = "What should we do during an earthquake?",
            japaneseKanji = "地震の時はどうすればいいですか？",
            romaji = "Jishin no toki wa dō sureba ii desu ka?"
        ),

        // ALLERGY
        EmergencyPhraseItem(
            id = "phrase-alg-reaction",
            category = EmergencyCategory.ALLERGY,
            spanishText = "Tengo una reacción alérgica grave",
            englishText = "I am having a severe allergic reaction",
            japaneseKanji = "重いアレルギー反応が出ています",
            romaji = "Omoi arerugī hannō ga dete imasu"
        ),
        EmergencyPhraseItem(
            id = "phrase-alg-nuts",
            category = EmergencyCategory.ALLERGY,
            spanishText = "Tengo alergia grave a los frutos secos",
            englishText = "I have a severe allergy to nuts",
            japaneseKanji = "重度のナッツアレルギーがあります",
            romaji = "Jūdo no nattsu arerugī ga arimasu"
        ),
        EmergencyPhraseItem(
            id = "phrase-alg-shellfish",
            category = EmergencyCategory.ALLERGY,
            spanishText = "Tengo alergia al marisco",
            englishText = "I am allergic to shellfish",
            japaneseKanji = "甲殻類アレルギーがあります",
            romaji = "Kōkakurui arerugī ga arimasu"
        ),
        EmergencyPhraseItem(
            id = "phrase-alg-egg-dairy",
            category = EmergencyCategory.ALLERGY,
            spanishText = "¿Contiene esto huevo o leche?",
            englishText = "Does this contain egg or milk?",
            japaneseKanji = "これに卵や乳製品は入っていますか？",
            romaji = "Kore ni tamago ya nyūseihin wa haitte imasu ka?"
        ),
        EmergencyPhraseItem(
            id = "phrase-alg-gluten",
            category = EmergencyCategory.ALLERGY,
            spanishText = "No puedo comer trigo ni gluten",
            englishText = "I cannot eat wheat or gluten",
            japaneseKanji = "小麦・グルテンは食べられません",
            romaji = "Komugi, guruten wa taberaremasen"
        )
    )
}
