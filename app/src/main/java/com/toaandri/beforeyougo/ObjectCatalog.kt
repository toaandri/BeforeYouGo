package com.toaandri.beforeyougo

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import java.text.Normalizer

data class CatalogObject(val key: String, private val nameResource: Int, val categoryResource: Int, val icon: ImageVector) {
    val name: String get() = uiText(nameResource)
    val category: String get() = uiText(categoryResource)
    fun matchesTitle(title: String): Boolean = listOf("en", "fr").any {
        normalized(AppText.inLanguage(nameResource, it)) == normalized(title)
    }
}

private val Glasses = ImageVector.Builder(uiText(R.string.ui_glasses), 24.dp, 24.dp, 24f, 24f).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f) {
        moveTo(2f, 12f); lineTo(4f, 5f); lineTo(7f, 5f)
        moveTo(22f, 12f); lineTo(20f, 5f); lineTo(17f, 5f)
        moveTo(10f, 13f); quadTo(12f, 11f, 14f, 13f)
        moveTo(2f, 11f); lineTo(10f, 11f); lineTo(10f, 15f); quadTo(10f, 19f, 6f, 19f)
        quadTo(2f, 19f, 2f, 15f); close()
        moveTo(14f, 11f); lineTo(22f, 11f); lineTo(22f, 15f); quadTo(22f, 19f, 18f, 19f)
        quadTo(14f, 19f, 14f, 15f); close()
    }
}.build()

val objectCatalog = listOf(
    CatalogObject("glasses", R.string.ui_glasses, R.string.ui_essentials, Glasses),
    CatalogObject("headphones", R.string.ui_headphones, R.string.ui_essentials, Icons.Outlined.Headphones),
    CatalogObject("keys", R.string.ui_keys, R.string.ui_essentials, Icons.Outlined.Key),
    CatalogObject("wallet", R.string.ui_wallet, R.string.ui_essentials, Icons.Outlined.AccountBalanceWallet),
    CatalogObject("phone", R.string.ui_phone, R.string.ui_essentials, Icons.Outlined.PhoneAndroid),
    CatalogObject("transport", R.string.ui_transport_card, R.string.ui_essentials, Icons.Outlined.DirectionsBus),
    CatalogObject("watch", R.string.ui_watch, R.string.ui_essentials, Icons.Outlined.Watch),
    CatalogObject("bag", R.string.ui_backpack, R.string.ui_essentials, Icons.Outlined.Backpack),
    CatalogObject("laptop", R.string.ui_laptop, R.string.ui_work_study, Icons.Outlined.Laptop),
    CatalogObject("charger", R.string.ui_charger, R.string.ui_work_study, Icons.Outlined.Power),
    CatalogObject("battery", R.string.ui_power_bank, R.string.ui_work_study, Icons.Outlined.BatteryChargingFull),
    CatalogObject("badge", R.string.ui_badge, R.string.ui_work_study, Icons.Outlined.Badge),
    CatalogObject("notebook", R.string.ui_notebook, R.string.ui_work_study, Icons.Outlined.MenuBook),
    CatalogObject("pen", R.string.ui_pen, R.string.ui_work_study, Icons.Outlined.Edit),
    CatalogObject("usb", R.string.ui_usb_drive, R.string.ui_work_study, Icons.Outlined.Usb),
    CatalogObject("documents", R.string.ui_documents, R.string.ui_work_study, Icons.Outlined.Description),
    CatalogObject("water", R.string.ui_water_bottle, R.string.ui_wellbeing, Icons.Outlined.WaterDrop),
    CatalogObject("lunch", R.string.ui_lunch, R.string.ui_wellbeing, Icons.Outlined.LunchDining),
    CatalogObject("medication", R.string.ui_medication, R.string.ui_wellbeing, Icons.Outlined.Medication),
    CatalogObject("tissues", R.string.ui_tissues, R.string.ui_wellbeing, Icons.Outlined.Sanitizer),
    CatalogObject("sanitizer", R.string.ui_hand_sanitizer, R.string.ui_wellbeing, Icons.Outlined.CleanHands),
    CatalogObject("sunscreen", R.string.ui_sunscreen, R.string.ui_wellbeing, Icons.Outlined.WbSunny),
    CatalogObject("umbrella", R.string.ui_umbrella, R.string.ui_outings_travel, Icons.Outlined.Umbrella),
    CatalogObject("sunglasses", R.string.ui_sunglasses, R.string.ui_outings_travel, Glasses),
    CatalogObject("passport", R.string.ui_passport, R.string.ui_outings_travel, Icons.Outlined.Book),
    CatalogObject("tickets", R.string.ui_tickets, R.string.ui_outings_travel, Icons.Outlined.ConfirmationNumber),
    CatalogObject("camera", R.string.ui_camera, R.string.ui_outings_travel, Icons.Outlined.PhotoCamera),
    CatalogObject("suitcase", R.string.ui_suitcase, R.string.ui_outings_travel, Icons.Outlined.Luggage),
    CatalogObject("sports", R.string.ui_sports_clothes, R.string.ui_sport_family, Icons.Outlined.FitnessCenter),
    CatalogObject("helmet", R.string.ui_bike_helmet, R.string.ui_sport_family, Icons.Outlined.SportsMotorsports),
    CatalogObject("towel", R.string.ui_towel, R.string.ui_sport_family, Icons.Outlined.DryCleaning),
    CatalogObject("swimming", R.string.ui_swimsuit, R.string.ui_sport_family, Icons.Outlined.Pool),
    CatalogObject("baby", R.string.ui_changing_bag, R.string.ui_sport_family, Icons.Outlined.ChildCare),
    CatalogObject("pet", R.string.ui_leash, R.string.ui_sport_family, Icons.Outlined.Pets),
    CatalogObject("snack", R.string.ui_snack, R.string.ui_sport_family, Icons.Outlined.Cookie),
    CatalogObject("toy", R.string.ui_comfort_toy, R.string.ui_sport_family, Icons.Outlined.Toys)
)

fun normalized(text: String): String = Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
    .replace("\\p{M}+".toRegex(), "").lowercase(java.util.Locale.ROOT)

fun iconFor(item: SavedItem): ImageVector = (objectCatalog.find { it.key == item.iconKey }
    ?: objectCatalog.find { it.matchesTitle(item.title) })?.icon ?: Icons.Outlined.Inventory2
