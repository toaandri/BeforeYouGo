package com.toaandri.beforeyougo

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import java.text.Normalizer

data class CatalogObject(val key: String, val name: String, val category: String, val icon: ImageVector)

private val Glasses = ImageVector.Builder("Lunettes", 24.dp, 24.dp, 24f, 24f).apply {
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
    CatalogObject("glasses", "Lunettes", "Essentiels", Glasses),
    CatalogObject("headphones", "Écouteurs", "Essentiels", Icons.Outlined.Headphones),
    CatalogObject("keys", "Clés", "Essentiels", Icons.Outlined.Key),
    CatalogObject("wallet", "Portefeuille", "Essentiels", Icons.Outlined.AccountBalanceWallet),
    CatalogObject("phone", "Téléphone", "Essentiels", Icons.Outlined.PhoneAndroid),
    CatalogObject("transport", "Carte de transport", "Essentiels", Icons.Outlined.DirectionsBus),
    CatalogObject("watch", "Montre", "Essentiels", Icons.Outlined.Watch),
    CatalogObject("bag", "Sac à dos", "Essentiels", Icons.Outlined.Backpack),
    CatalogObject("laptop", "Ordinateur", "Travail & études", Icons.Outlined.Laptop),
    CatalogObject("charger", "Chargeur", "Travail & études", Icons.Outlined.Power),
    CatalogObject("battery", "Batterie externe", "Travail & études", Icons.Outlined.BatteryChargingFull),
    CatalogObject("badge", "Badge", "Travail & études", Icons.Outlined.Badge),
    CatalogObject("notebook", "Carnet", "Travail & études", Icons.Outlined.MenuBook),
    CatalogObject("pen", "Stylo", "Travail & études", Icons.Outlined.Edit),
    CatalogObject("usb", "Clé USB", "Travail & études", Icons.Outlined.Usb),
    CatalogObject("documents", "Documents", "Travail & études", Icons.Outlined.Description),
    CatalogObject("water", "Gourde", "Bien-être", Icons.Outlined.WaterDrop),
    CatalogObject("lunch", "Repas", "Bien-être", Icons.Outlined.LunchDining),
    CatalogObject("medication", "Médicaments", "Bien-être", Icons.Outlined.Medication),
    CatalogObject("tissues", "Mouchoirs", "Bien-être", Icons.Outlined.Sanitizer),
    CatalogObject("sanitizer", "Gel désinfectant", "Bien-être", Icons.Outlined.CleanHands),
    CatalogObject("sunscreen", "Crème solaire", "Bien-être", Icons.Outlined.WbSunny),
    CatalogObject("umbrella", "Parapluie", "Sorties & voyage", Icons.Outlined.Umbrella),
    CatalogObject("sunglasses", "Lunettes de soleil", "Sorties & voyage", Glasses),
    CatalogObject("passport", "Passeport", "Sorties & voyage", Icons.Outlined.Book),
    CatalogObject("tickets", "Billets", "Sorties & voyage", Icons.Outlined.ConfirmationNumber),
    CatalogObject("camera", "Appareil photo", "Sorties & voyage", Icons.Outlined.PhotoCamera),
    CatalogObject("suitcase", "Valise", "Sorties & voyage", Icons.Outlined.Luggage),
    CatalogObject("sports", "Tenue de sport", "Sport & famille", Icons.Outlined.FitnessCenter),
    CatalogObject("helmet", "Casque vélo", "Sport & famille", Icons.Outlined.SportsMotorsports),
    CatalogObject("towel", "Serviette", "Sport & famille", Icons.Outlined.DryCleaning),
    CatalogObject("swimming", "Maillot de bain", "Sport & famille", Icons.Outlined.Pool),
    CatalogObject("baby", "Sac à langer", "Sport & famille", Icons.Outlined.ChildCare),
    CatalogObject("pet", "Laisse", "Sport & famille", Icons.Outlined.Pets),
    CatalogObject("snack", "Goûter", "Sport & famille", Icons.Outlined.Cookie),
    CatalogObject("toy", "Doudou", "Sport & famille", Icons.Outlined.Toys)
)

fun normalized(text: String): String = Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
    .replace("\\p{M}+".toRegex(), "").lowercase(java.util.Locale.ROOT)

fun iconFor(item: SavedItem): ImageVector = (objectCatalog.find { it.key == item.iconKey }
    ?: objectCatalog.find { normalized(it.name) == normalized(item.title) })?.icon ?: Icons.Outlined.Inventory2
