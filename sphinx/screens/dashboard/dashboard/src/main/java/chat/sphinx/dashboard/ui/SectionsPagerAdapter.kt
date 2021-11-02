package chat.sphinx.dashboard.ui

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import chat.sphinx.dashboard.R

private val TAB_TITLES = arrayOf(
    R.string.dashboard_feed_tab_name,
    R.string.dashboard_friends_tab_name,
    R.string.dashboard_tribes_tab_name,
)

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter(private val context: Context, fm: FragmentManager) :
    FragmentPagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> {
                // TODO: Feed Fragment...
                ChatListFragment.newInstance()
            }
            1 -> {
                ChatListFragment.newInstance(
                    chatListType = ChatListFragment.CONTACT_CHAT_LIST
                )
            }
            2 -> {
                ChatListFragment.newInstance(
                    chatListType = ChatListFragment.TRIBE_CHAT_LIST
                )
            }
            else ->  {
                ChatListFragment.newInstance()
            }
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return context.resources.getString(TAB_TITLES[position])
    }

    override fun getCount(): Int {
        return TAB_TITLES.size
    }
}