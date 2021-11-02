package chat.sphinx.dashboard.ui

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.FragmentChatListBinding
import chat.sphinx.dashboard.ui.adapter.ChatListAdapter
import chat.sphinx.dashboard.ui.adapter.ChatListFooterAdapter
import chat.sphinx.dashboard.ui.viewstates.ChatFilter
import chat.sphinx.dashboard.ui.viewstates.NavDrawerViewState
import chat.sphinx.resources.SphinxToastUtils
import chat.sphinx.resources.inputMethodManager
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.motionlayout.MotionLayoutFragment
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.updateViewState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("NOTHING_TO_INLINE")
private inline fun FragmentChatListBinding.searchBarClearFocus() {
    layoutSearchBar.editTextDashboardSearch.clearFocus()
}

@AndroidEntryPoint
internal class ChatListFragment : MotionLayoutFragment<
        Any,
        Context,
        ChatListSideEffect,
        NavDrawerViewState,
        ChatListViewModel,
        FragmentChatListBinding
        >(R.layout.fragment_chat_list), SwipeRefreshLayout.OnRefreshListener
{
    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var userColorsHelper: UserColorsHelper

    override val viewModel: ChatListViewModel by viewModels()
    override val binding: FragmentChatListBinding by viewBinding(FragmentChatListBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        BackPressHandler(binding.root.context)
            .enableDoubleTapToClose(viewLifecycleOwner, SphinxToastUtils())
            .addCallback(viewLifecycleOwner, requireActivity())

        viewModel.networkRefresh()

        findNavController().addOnDestinationChangedListener(CloseDrawerOnDestinationChange())

        setupChats()
        setupSearch()
    }

    override fun onResume() {
        super.onResume()

        activity?.intent?.dataString?.let { deepLink ->
            viewModel.handleDeepLink(deepLink)
            activity?.intent?.data = null
        }
    }

    private inner class BackPressHandler(context: Context): CloseAppOnBackPress(context) {
        override fun handleOnBackPressed() {
            if (viewModel.currentViewState is NavDrawerViewState.Open) {
                viewModel.updateViewState(NavDrawerViewState.Closed)
            } else {
                binding.searchBarClearFocus()
                super.handleOnBackPressed()
            }
        }
    }

    private inner class CloseDrawerOnDestinationChange: NavController.OnDestinationChangedListener {
        override fun onDestinationChanged(
            controller: NavController,
            destination: NavDestination,
            arguments: Bundle?
        ) {
            controller.removeOnDestinationChangedListener(this)
            viewModel.updateViewState(NavDrawerViewState.Closed)
        }
    }

    override fun onRefresh() {
        binding.layoutChatListChats.layoutSwipeRefreshChats.isRefreshing = false
        viewModel.networkRefresh()
    }

    private fun setupChats() {
        binding.layoutChatListChats.layoutSwipeRefreshChats.setOnRefreshListener(this)

        binding.layoutChatListChats.recyclerViewChats.apply {
            val linearLayoutManager = LinearLayoutManager(context)
            val chatListAdapter = ChatListAdapter(
                this,
                linearLayoutManager,
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel,
                userColorsHelper
            )

            val chatListFooterAdapter = ChatListFooterAdapter(viewLifecycleOwner, onStopSupervisor, viewModel)
            this.setHasFixedSize(false)
            layoutManager = linearLayoutManager
            adapter = ConcatAdapter(chatListAdapter, chatListFooterAdapter)
            itemAnimator = null
        }
    }

    private fun setupSearch() {
        binding.layoutSearchBar.apply {
            editTextDashboardSearch.addTextChangedListener { editable ->
                buttonDashboardSearchClear.goneIfFalse(editable.toString().isNotEmpty())

                onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                    viewModel.updateChatListFilter(
                        if (editable.toString().isNotEmpty()) {
                            ChatFilter.FilterBy(editable.toString())
                        } else {
                            ChatFilter.ClearFilter
                        }
                    )
                }
            }

            editTextDashboardSearch.setOnEditorActionListener(object: TextView.OnEditorActionListener {
                override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
                    if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                        editTextDashboardSearch.let { editText ->
                            binding.root.context.inputMethodManager?.let { imm ->
                                if (imm.isActive(editText)) {
                                    imm.hideSoftInputFromWindow(editText.windowToken, 0)
                                    editText.clearFocus()
                                }
                            }
                        }
                        return true
                    }
                    return false
                }
            })

            buttonDashboardSearchClear.setOnClickListener {
                editTextDashboardSearch.setText("")
            }
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onPause() {
        super.onPause()
        binding.searchBarClearFocus()
    }

    override suspend fun onViewStateFlowCollect(viewState: NavDrawerViewState) {
        @Exhaustive
        when (viewState) {
            NavDrawerViewState.Closed -> {
                binding.layoutMotionDashboard.setTransitionDuration(150)
            }
            NavDrawerViewState.Open -> {
                binding.layoutMotionDashboard.setTransitionDuration(300)
                binding.layoutSearchBar.editTextDashboardSearch.let { editText ->
                    binding.root.context.inputMethodManager?.let { imm ->
                        if (imm.isActive(editText)) {
                            imm.hideSoftInputFromWindow(editText.windowToken, 0)
                            delay(250L)
                        }
                    }
                    binding.searchBarClearFocus()
                }
            }
        }
        viewState.transitionToEndSet(binding.layoutMotionDashboard)
    }

    override fun onViewCreatedRestoreMotionScene(
        viewState: NavDrawerViewState,
        binding: FragmentChatListBinding
    ) {
        viewState.restoreMotionScene(binding.layoutMotionDashboard)
    }

    override fun getMotionLayouts(): Array<MotionLayout> {
        return arrayOf(binding.layoutMotionDashboard)
    }

    override suspend fun onSideEffectCollect(sideEffect: ChatListSideEffect) {
        sideEffect.execute(binding.root.context)
    }

    companion object {
        const val TRIBE_CHAT_LIST = 1
        const val CONTACT_CHAT_LIST = 0

        fun newInstance(
            updateBackgroundLoginTime: Boolean = false,
            chatListType: Int = 0,
            deepLink: String? = null,
        ): ChatListFragment {
            return ChatListFragment().apply {
                val args = ChatListFragmentArgs.Builder(updateBackgroundLoginTime, chatListType)
                args.argDeepLink = deepLink

                arguments = args.build().toBundle()
            }
        }
    }
}