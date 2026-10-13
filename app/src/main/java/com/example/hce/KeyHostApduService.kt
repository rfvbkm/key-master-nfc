package com.example.hce

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

class KeyHostApduService : HostApduService() {

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        return ApduProcessor.processCommand(commandApdu)
    }

    override fun onDeactivated(reason: Int) {
        val desc = when (reason) {
            DEACTIVATION_LINK_LOSS -> "Поле считывателя потеряно (карта отдалена)"
            DEACTIVATION_DESELECTED -> "Деселекция (выбрано другое AID)"
            else -> "Сеанс завершен (код $reason)"
        }
        EmulationManager.logInteraction("-", "-", desc)
    }
}
