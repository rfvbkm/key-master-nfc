package com.example.model

import org.json.JSONArray
import org.json.JSONObject

object DumpSerializer {

    fun toJson(dump: CardDump): String {
        val root = JSONObject()
        root.put("uid", dump.uid)
        root.put("atqa", dump.atqa)
        root.put("sak", dump.sak)
        root.put("cardType", dump.cardType.name)
        root.put("readTimestamp", dump.readTimestamp)
        dump.ndefPayload?.let { root.put("ndefPayload", it) }

        val techsArray = JSONArray()
        dump.rawTechs.forEach { techsArray.put(it) }
        root.put("rawTechs", techsArray)

        // Sectors
        val sectorsArray = JSONArray()
        dump.sectors.forEach { sector ->
            val secObj = JSONObject()
            secObj.put("sectorIndex", sector.sectorIndex)
            secObj.put("isReadSuccess", sector.isReadSuccess)
            sector.keyA?.let { secObj.put("keyA", it) }
            sector.keyB?.let { secObj.put("keyB", it) }
            sector.accessBits?.let { secObj.put("accessBits", it) }

            val blocksArray = JSONArray()
            sector.blocks.forEach { block ->
                blocksArray.put(block.toHex())
            }
            secObj.put("blocks", blocksArray)
            sectorsArray.put(secObj)
        }
        root.put("sectors", sectorsArray)

        // Pages
        val pagesArray = JSONArray()
        dump.pages.forEach { page ->
            val pObj = JSONObject()
            pObj.put("pageNumber", page.pageNumber)
            pObj.put("dataHex", page.data.toHex())
            pagesArray.put(pObj)
        }
        root.put("pages", pagesArray)

        return root.toString(2)
    }

    fun fromJson(jsonStr: String): CardDump {
        val root = JSONObject(jsonStr)
        val uid = root.optString("uid", "UNKNOWN")
        val atqa = root.optString("atqa", "")
        val sak = root.optString("sak", "")
        val typeName = root.optString("cardType", CardType.UNKNOWN.name)
        val cardType = try { CardType.valueOf(typeName) } catch (_: Exception) { CardType.UNKNOWN }
        val readTimestamp = root.optLong("readTimestamp", System.currentTimeMillis())
        val ndefPayload = if (root.has("ndefPayload")) root.getString("ndefPayload") else null

        val techsList = mutableListOf<String>()
        val techsArray = root.optJSONArray("rawTechs")
        if (techsArray != null) {
            for (i in 0 until techsArray.length()) {
                techsList.add(techsArray.getString(i))
            }
        }

        val sectorsList = mutableListOf<SectorData>()
        val sectorsArray = root.optJSONArray("sectors")
        if (sectorsArray != null) {
            for (i in 0 until sectorsArray.length()) {
                val secObj = sectorsArray.getJSONObject(i)
                val sIndex = secObj.getInt("sectorIndex")
                val isSuccess = secObj.optBoolean("isReadSuccess", true)
                val keyA = if (secObj.has("keyA")) secObj.getString("keyA") else null
                val keyB = if (secObj.has("keyB")) secObj.getString("keyB") else null
                val accessBits = if (secObj.has("accessBits")) secObj.getString("accessBits") else null

                val blocks = mutableListOf<ByteArray>()
                val blocksArray = secObj.getJSONArray("blocks")
                for (b in 0 until blocksArray.length()) {
                    blocks.add(blocksArray.getString(b).hexToByteArray())
                }
                sectorsList.add(
                    SectorData(
                        sectorIndex = sIndex,
                        blocks = blocks,
                        keyA = keyA,
                        keyB = keyB,
                        accessBits = accessBits,
                        isReadSuccess = isSuccess
                    )
                )
            }
        }

        val pagesList = mutableListOf<PageData>()
        val pagesArray = root.optJSONArray("pages")
        if (pagesArray != null) {
            for (i in 0 until pagesArray.length()) {
                val pObj = pagesArray.getJSONObject(i)
                val pageNum = pObj.getInt("pageNumber")
                val data = pObj.getString("dataHex").hexToByteArray()
                pagesList.add(PageData(pageNum, data))
            }
        }

        return CardDump(
            uid = uid,
            atqa = atqa,
            sak = sak,
            cardType = cardType,
            sectors = sectorsList,
            pages = pagesList,
            ndefPayload = ndefPayload,
            rawTechs = techsList,
            readTimestamp = readTimestamp
        )
    }

    /**
     * Exports MIFARE Classic dump to standard MCT (.mct) format text
     */
    fun toMctFormat(dump: CardDump): String {
        val sb = StringBuilder()
        sb.append("+Sector: 0\n")
        dump.sectors.forEach { sec ->
            if (sec.sectorIndex > 0) {
                sb.append("\n+Sector: ${sec.sectorIndex}\n")
            }
            sec.blocks.forEach { block ->
                sb.append(block.toHex()).append("\n")
            }
        }
        return sb.toString()
    }
}
