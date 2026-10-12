package com.toaandri.beforeyougo

import android.app.Application
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.activity.ComponentActivity
import java.util.Locale

class BeforeYouGoApplication : Application() {
    override fun onCreate() { super.onCreate(); AppText.initialize(this) }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig); AppText.invalidate()
    }
}

object AppText {
    private var application: Context? = null
    private var cachedTag: String? = null
    private var cachedResources: Resources? = null
    fun initialize(context: Context) {
        application = context.applicationContext
        if (Build.VERSION.SDK_INT >= 33) {
            val preferences = context.getSharedPreferences("before_you_go", Context.MODE_PRIVATE)
            if (!preferences.getBoolean("native_language_migrated", false)) {
                val manager = context.getSystemService(LocaleManager::class.java)
                val saved = preferences.getString("language_preference", null)
                if (manager.applicationLocales.isEmpty && saved in listOf("en", "fr"))
                    manager.applicationLocales = LocaleList.forLanguageTags(saved)
                preferences.edit().putBoolean("native_language_migrated", true).apply()
            }
        }
        invalidate()
    }
    fun invalidate() { cachedTag = null; cachedResources = null }
    private fun language(context: Context): String {
        if (Build.VERSION.SDK_INT >= 33) {
            val systemChoice = context.getSystemService(LocaleManager::class.java).applicationLocales
            return if (!systemChoice.isEmpty && systemChoice[0].language == "fr") "fr" else "en"
        }
        return context.getSharedPreferences("before_you_go", Context.MODE_PRIVATE)
            .getString("language_preference", "en").let { if (it == "fr") "fr" else "en" }
    }
    val locale: Locale get() = Locale.forLanguageTag(application?.let(::language) ?: "en")
    fun localizedContext(context: Context, tag: String = language(context)): Context {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocales(LocaleList.forLanguageTags(tag))
        return context.createConfigurationContext(configuration)
    }
    fun selectLanguage(context: Context, tag: String) {
        require(tag == "en" || tag == "fr")
        context.getSharedPreferences("before_you_go", Context.MODE_PRIVATE).edit()
            .putString("language_preference", tag).apply()
        invalidate()
        if (Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(tag)
        } else (context as? ComponentActivity)?.recreate()
    }
    fun inLanguage(id: Int, tag: String, vararg arguments: Any): String {
        val context = application
        if (context == null) return String.format(Locale.ENGLISH, english[id] ?: "", *arguments)
        val resources = if (cachedTag == tag && cachedResources != null) cachedResources!! else
            localizedContext(context, tag).resources.also { cachedTag = tag; cachedResources = it }
        return if (arguments.isEmpty()) resources.getString(id) else resources.getString(id, *arguments)
    }
    fun text(id: Int, vararg arguments: Any) = inLanguage(id, locale.language, *arguments)
    // Plain fallback values allow pure model tests without constructing Android resources.
    private val english = mapOf(
        R.string.ui_custom_ringtone to "Custom ringtone",
        R.string.ui_a_departure_page to "A departure page",
        R.string.ui_where_are_we_going to "Where are we going?",
        R.string.ui_a_small_ritual_before_you_go_at_your_own_pace to "A small ritual before you go. At your own pace.",
        R.string.ui_name_this_departure to "Name this departure…",
        R.string.ui_work_a_weekend_away_a_workout to "Work, a weekend away, a workout…",
        R.string.ui_when_should_we_give_you_a_nudge to "When should we give you a nudge?",
        R.string.ui_change_departure_time to "Change departure time",
        R.string.ui_choose_your_moment_a_little_before_leaving_if_you_prefer to "Choose your moment, a little before leaving if you prefer.",
        R.string.ui_the_days_that_fit to "The days that fit",
        R.string.ui_mon to "Mon",
        R.string.ui_tue to "Tue",
        R.string.ui_wed to "Wed",
        R.string.ui_thu to "Thu",
        R.string.ui_fri to "Fri",
        R.string.ui_sat to "Sat",
        R.string.ui_sun to "Sun",
        R.string.ui_every_day to "Every day",
        R.string.ui_a_sound_with_stop_and_snooze_controls to "A sound with stop and snooze controls.",
        R.string.ui_a_quick_check_then_off_you_go to "%1\$s · A quick check, then off you go.",
        R.string.ui_your_departure to "Your departure",
        R.string.ui_sound_your_way to "Sound, your way",
        R.string.ui_departure_sound to "Departure sound",
        R.string.ui_the_ringtone_picker_is_unavailable_on_this_phone to "The ringtone picker is unavailable on this phone.",
        R.string.ui_with_these_settings_only_a_notification_will_appear to "With these settings, only a notification will appear.",
        R.string.ui_how_long_to_wait_when_you_want_to_come_back to "How long to wait when you want to come back later.",
        R.string.ui_to_bring_along to "To bring along",
        R.string.ui_new_objects_also_join_this_departure_starting_on_their_chosen to "New objects also join this departure, starting on their chosen date.",
        R.string.ui_compose_this_departure_bag_dated_objects_appear_from_their_scheduled to "Compose this departure bag. Dated objects appear from their scheduled day.",
        R.string.ui_object_selected to "%1\$s object selected",
        R.string.ui_save_this_departure to "Save this departure",
        R.string.ui_departure_sounds to "Departure sounds",
        R.string.ui_scheduled_alarms_with_stop_and_snooze_controls to "Scheduled alarms with stop and snooze controls",
        R.string.ui_stop_the_sound to "Stop the sound",
        R.string.ui_phone_ringtone to "Phone ringtone",
        R.string.ui_follow_system to "Follow system",
        R.string.ui_light to "Light",
        R.string.ui_dark to "Dark",
        R.string.ui_a_sound_if_you_want_one_stop_or_snooze_it to "A sound if you want one. Stop or snooze it whenever you like.",
        R.string.ui_a_quiet_invitation_to_check_your_bag_open_it_when to "A quiet invitation to check your bag. Open it when you want.",
        R.string.ui_my_departure to "My departure",
        R.string.ui_my_bag_for_today to "My bag for today",
        R.string.ui_everything_is_here to "Everything is here.",
        R.string.ui_of_on_board to "%1\$s of %2\$s on board",
        R.string.ui_a_departure_that_feels_like_you to "A departure that feels like you.",
        R.string.ui_a_book_a_water_bottle_your_keys_choose_what_comes to "A book, a water bottle, your keys… Choose what comes with you.",
        R.string.ui_choose_my_first_objects to "Choose my first objects",
        R.string.ui_an_object to "An object",
        R.string.ui_tap_an_object_or_hold_it_to_drag_it_into to "Tap an object, or hold it to drag it into the bag. Tap again to take it out.",
        R.string.ui_in_the_bag_remove to "In the bag · remove",
        R.string.ui_your_bag_your_choices_leave_whenever_you_want to "Your bag, your choices. Leave whenever you want.",
        R.string.ui_your_departure_is_saved_invitations_are_disabled_your_bag_is to "Your departure is saved. Invitations are disabled; your bag is always available.",
        R.string.ui_your_departure_is_saved_allow_exact_alarms_in_android_to to "Your departure is saved. Allow exact alarms in Android to receive a nudge at the chosen time. You can also use your bag without notifications.",
        R.string.ui_my_preferences to "My preferences",
        R.string.ui_add_a_personal_object to "Add a personal object",
        R.string.ui_back_to_today_s_bag to "Back to today’s bag",
        R.string.ui_your_departure_57fd9c to "Your departure · %1\$s",
        R.string.ui_your_departures_are_saved_enable_the_android_permissions_to_receive to "Your departures are saved. Enable the Android permissions to receive their invitations.",
        R.string.ui_choose_my_permissions to "Choose my permissions",
        R.string.ui_your_bag_appointments to "Your bag appointments",
        R.string.ui_a_little_nudge_when_it_suits_you to "A little nudge, when it suits you.",
        R.string.ui_plan_a_departure to "Plan a departure",
        R.string.ui_the_next_part_is_yours to "The next part is yours.",
        R.string.ui_your_bag_is_ready_have_a_good_journey to "Your bag is ready. Have a good journey!",
        R.string.ui_have_a_good_journey_your_bag_will_be_here_whenever to "Have a good journey! Your bag will be here whenever you want.",
        R.string.ui_see_you_soon to "See you soon",
        R.string.ui_delete to "Delete “%1\$s”?",
        R.string.ui_this_appointment_will_be_cancelled_your_objects_stay_in_your to "This appointment will be cancelled. Your objects stay in your collection.",
        R.string.ui_delete_16ce0b to "Delete",
        R.string.ui_cancel to "Cancel",
        R.string.ui_your_departure_is_saved to "Your departure is saved",
        R.string.ui_open_settings to "Open settings",
        R.string.ui_open_this_bag to "Open this bag",
        R.string.ui to "%1\$s · %2\$s",
        R.string.ui_vibration to "Vibration",
        R.string.ui_delete_ecb441 to "Delete %1\$s",
        R.string.ui_cancel_the_current_snooze to "Cancel the current snooze",
        R.string.ui_restore_the_next_appointment to "Restore the next appointment",
        R.string.ui_try_the_sound_10_s to "Try the sound · 10 s",
        R.string.ui_your_way to "Your way.",
        R.string.ui_your_bag_is_available_even_without_notifications to "Your bag is available even without notifications.",
        R.string.ui_exact_alarms to "Exact alarms: %1\$s",
        R.string.ui_allowed to "allowed",
        R.string.ui_permission_needed to "permission needed",
        R.string.ui_notifications to "Notifications: %1\$s",
        R.string.ui_notification_settings to "Notification settings",
        R.string.ui_new_departures_use_a_quiet_nudge_by_default_you_can to "New departures use a quiet nudge by default. You can choose a sound for each one: alarm volume, automatic stop after 2 minutes and a 10-second preview.",
        R.string.ui_phone_volume_and_sounds to "Phone volume and sounds",
        R.string.ui_your_rhythm_your_departures to "Your rhythm, your departures",
        R.string.ui_in_my_departure_plan_an_appointment_for_work_swimming_or to "In My departure, plan an appointment for work, swimming or a weekend outing. Each has its own days and objects. Skip an appointment, pause a departure or snooze an invitation.",
        R.string.ui_no_account_ads_or_tracking_your_objects_and_departures_stay to "No account, ads or tracking. Your objects and departures stay on this phone. Uninstalling removes them.",
        R.string.ui_essentials to "Essentials",
        R.string.ui_headphones to "Headphones",
        R.string.ui_keys to "Keys",
        R.string.ui_wallet to "Wallet",
        R.string.ui_phone to "Phone",
        R.string.ui_watch to "Watch",
        R.string.ui_backpack to "Backpack",
        R.string.ui_work_study to "Work & study",
        R.string.ui_notebook to "Notebook",
        R.string.ui_pen to "Pen",
        R.string.ui_usb_drive to "USB drive",
        R.string.ui_documents to "Documents",
        R.string.ui_water_bottle to "Water bottle",
        R.string.ui_wellbeing to "Wellbeing",
        R.string.ui_lunch to "Lunch",
        R.string.ui_medication to "Medication",
        R.string.ui_hand_sanitizer to "Hand sanitizer",
        R.string.ui_sunscreen to "Sunscreen",
        R.string.ui_umbrella to "Umbrella",
        R.string.ui_passport to "Passport",
        R.string.ui_tickets to "Tickets",
        R.string.ui_suitcase to "Suitcase",
        R.string.ui_sport_family to "Sport & family",
        R.string.ui_bike_helmet to "Bike helmet",
        R.string.ui_towel to "Towel",
        R.string.ui_changing_bag to "Changing bag",
        R.string.ui_leash to "Leash",
        R.string.ui_snack to "Snack",
        R.string.ui_comfort_toy to "Comfort toy",
        R.string.ui_all to "All",
        R.string.ui_browse_ideas to "Browse ideas",
        R.string.ui_search_objects to "Search objects",
        R.string.ui_the_first_page_is_yours to "The first page is yours.",
        R.string.ui_browse_a_few_ideas_or_create_your_own_object_with to "Browse a few ideas or create your own object with +.",
        R.string.ui_try_another_word_or_add_this_object_with to "Try another word, or add this object with +.",
        R.string.ui_explore_the_ideas to "Explore the ideas",
        R.string.ui_from to "From %1\$s",
        R.string.ui_delete_b7c5a9 to "Delete %1\$s",
        R.string.ui_ideas_to_make_your_own to "%1\$s ideas to make your own",
        R.string.ui_no_idea_under_that_name_yet to "No idea under that name yet.",
        R.string.ui_choose_another_category_or_create_your_own_object_with to "Choose another category or create your own object with +.",
        R.string.ui_remove_this_object to "Remove this object?",
        R.string.ui_will_leave_your_collection_and_departure_bags to "“%1\$s” will leave your collection and departure bags.",
        R.string.ui_something_to_bring to "Something to bring",
        R.string.ui_an_object_your_way to "An object, your way",
        R.string.ui_my_notebook_lea_s_book to "My notebook, Lea’s book…",
        R.string.ui_for_a_particular_occasion to "For a particular occasion.",
        R.string.ui_from_ccd57b to "From %1\$s",
        R.string.ui_it_appears_from_this_date_and_stays_available_until_the to "It appears from this date and stays available until the day after you confirm it.",
        R.string.ui_the_detail_that_suits_it to "The detail that suits it",
        R.string.ui_save to "Save",
        R.string.ui_quiet_departure_nudges to "Quiet departure nudges",
        R.string.ui_quiet_invitations_to_check_your_bag_before_leaving to "Quiet invitations to check your bag before leaving",
        R.string.ui_open_my_departure to "Open my departure",
        R.string.ui_silent to "Silent",
        R.string.ui_close to "Close",
        R.string.ui_the_appointment to "The appointment",
        R.string.ui_weekdays to "Weekdays",
        R.string.ui_weekend to "Weekend",
        R.string.ui_choose_at_least_one_day_for_this_appointment to "Choose at least one day for this appointment.",
        R.string.ui_your_little_nudge to "Your little nudge",
        R.string.ui_quiet_or_with_a_sound_you_choose to "Quiet or with a sound, you choose.",
        R.string.ui_a_quiet_note to "A quiet note",
        R.string.ui_a_sound to "A sound",
        R.string.ui_a_notification_without_sound_or_vibration to "A notification without sound or vibration.",
        R.string.ui_a_preview_of_the_tone to "A preview of the tone",
        R.string.ui_shall_we_check_the_bag to "Shall we check the bag?",
        R.string.ui_with_vibration to "With vibration",
        R.string.ui_a_few_more_minutes to "A few more minutes?",
        R.string.ui_min to "%1\$s min",
        R.string.ui_my_whole_daily_collection to "My whole daily collection",
        R.string.ui_your_collection_is_empty_add_objects_in_the_my_objects to "Your collection is empty. Add objects in the My objects tab.",
        R.string.ui_objects_selected to "%1\$s objects selected",
        R.string.ui_preview to "Preview · %1\$s",
        R.string.ui_does_this_sound_suit_you to "Does this sound suit you?",
        R.string.ui_shall_we_check_before_you_go to "Shall we check before you go?",
        R.string.ui_in_min to "In %1\$s min",
        R.string.ui_with_a_sound to "With a sound",
        R.string.ui_quiet_nudge to "Quiet nudge",
        R.string.ui_monday_to_friday to "Monday to Friday",
        R.string.ui_at_the_weekend to "At the weekend",
        R.string.ui_over_here to "Over here!",
        R.string.ui_there_is_room_for_what_you_want to "There is room for what you want.",
        R.string.ui_with_me to "With me",
        R.string.ui_for_today to "For today",
        R.string.ui_off_i_go to "Off I go!",
        R.string.ui_my_objects to "My objects",
        R.string.ui_a_small_ritual_before_what_comes_next to "A small ritual before what comes next.",
        R.string.ui_sound_preview_10_seconds to "Sound preview · 10 seconds",
        R.string.ui_snooze_for_minutes to "Snooze for %1\$s minutes",
        R.string.ui_fancy_a_little_nudge to "Fancy a little nudge?",
        R.string.ui_later to "Later",
        R.string.ui_invitation_for to "Invitation for %1\$s",
        R.string.ui_invitation_paused to "Invitation paused",
        R.string.ui_invitation_needs_permission to "Invitation needs permission",
        R.string.ui_next_nudge to "Next nudge: %1\$s",
        R.string.ui_hide_options_for to "Hide options for %1\$s",
        R.string.ui_options_for to "Options for %1\$s",
        R.string.ui_quiet_nudge_silent_notification to "Quiet nudge · silent notification",
        R.string.ui_no_sound to "No sound",
        R.string.ui_no_vibration to "No vibration",
        R.string.ui_customize to "Customize",
        R.string.ui_skip_the_next_appointment to "Skip the next appointment",
        R.string.ui_appearance to "Appearance",
        R.string.ui_a_nudge_if_you_want to "A nudge, if you want",
        R.string.ui_allow_exact_alarms to "Allow exact alarms",
        R.string.ui_your_notebook_stays_here to "Your notebook stays here",
        R.string.ui_glasses to "Glasses",
        R.string.ui_transport_card to "Transport card",
        R.string.ui_laptop to "Laptop",
        R.string.ui_charger to "Charger",
        R.string.ui_power_bank to "Power bank",
        R.string.ui_badge to "Badge",
        R.string.ui_tissues to "Tissues",
        R.string.ui_outings_travel to "Outings & travel",
        R.string.ui_sunglasses to "Sunglasses",
        R.string.ui_camera to "Camera",
        R.string.ui_sports_clothes to "Sports clothes",
        R.string.ui_swimsuit to "Swimsuit",
        R.string.ui_the_little_things to "The little things",
        R.string.ui_a_little_world_of_your_own to "A little world of your own.",
        R.string.ui_your_essentials_your_wishes_what_comes_with_you to "Your essentials, your wishes, what comes with you.",
        R.string.ui_my_collection to "My collection · %1\$s",
        R.string.ui_clear_search to "Clear search",
        R.string.ui_nothing_under_that_name_for_now to "Nothing under that name for now.",
        R.string.ui_edit to "Edit %1\$s",
        R.string.ui_every_day_e8bdcc to "Every day",
        R.string.ui_in_my_collection to "✓ In my collection",
        R.string.ui_i_ll_bring_it to "I’ll bring it",
        R.string.ui_remove to "Remove",
        R.string.ui_keep to "Keep",
        R.string.ui_its_name to "Its name",
        R.string.ui_an_everyday_companion to "An everyday companion",
        R.string.ui_it_finds_its_place_every_day to "It finds its place every day.",
        R.string.ui_add to "Add",
        R.string.ui_a_quick_check_then_off_you_go_d5575c to "%1\$s · A quick check, then off you go.",
        R.string.ui_not_today to "Not today"
    )
}

fun uiText(id: Int, vararg arguments: Any): String = AppText.text(id, *arguments)
