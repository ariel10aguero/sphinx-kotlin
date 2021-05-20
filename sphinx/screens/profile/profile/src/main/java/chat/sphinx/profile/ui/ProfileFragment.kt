package chat.sphinx.profile.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.profile.R
import chat.sphinx.profile.databinding.FragmentProfileBinding
import chat.sphinx.profile.navigation.ProfileNavigator
import chat.sphinx.wrapper_common.lightning.asFormattedString
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@AndroidEntryPoint
internal class ProfileFragment: BaseFragment<
        ProfileViewState,
        ProfileViewModel,
        FragmentProfileBinding
        >(R.layout.fragment_profile)
{
    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override val viewModel: ProfileViewModel by viewModels()
    override val binding: FragmentProfileBinding by viewBinding(FragmentProfileBinding::bind)

    private val header: ConstraintLayout
        get() = binding.layoutProfileHeader.layoutConstraintProfileHeader
    private val headerNavBack: TextView
        get() = binding.layoutProfileHeader.textViewProfileHeaderNavBack

    sealed class ProfileTab() {
        object Basic: ProfileTab()
        object Advanced: ProfileTab()
    }

    @Inject
    protected lateinit var profileNavigator: ProfileNavigator

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        headerNavBack.setOnClickListener {
            lifecycleScope.launch {
                profileNavigator.popBackStack()
            }
        }

        setupProfileHeader()
        setupProfileTabs()
        setupProfile()
    }

    private fun setupProfileTabs() {
        showTabContainer(ProfileTab.Basic)

        binding.layoutProfileTabsHolder.basicTabButton.setOnClickListener {
            showTabContainer(ProfileTab.Basic)
        }

        binding.layoutProfileTabsHolder.advancedTabButton.setOnClickListener {
            showTabContainer(ProfileTab.Advanced)
        }
    }

    private fun showTabContainer(tab: ProfileTab) {
        @Exhaustive
        when (tab) {
            is ProfileTab.Basic -> {
                binding.layoutProfileTabsHolder.basicTabButton.setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.primaryBlue))
                binding.layoutProfileTabsHolder.advancedTabButton.setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.body))

                binding.layoutProfileBasicContainerHolder.layoutConstraintContainerHolder.goneIfFalse(true)
                binding.layoutProfileAdvancedContainerHolder.layoutConstraintContainerHolder.goneIfFalse(false)
            }
            is ProfileTab.Advanced -> {
                binding.layoutProfileTabsHolder.basicTabButton.setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.body))
                binding.layoutProfileTabsHolder.advancedTabButton.setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.primaryBlue))

                binding.layoutProfileBasicContainerHolder.layoutConstraintContainerHolder.goneIfFalse(false)
                binding.layoutProfileAdvancedContainerHolder.layoutConstraintContainerHolder.goneIfFalse(true)
            }
        }
    }

    private fun setupProfileHeader() {
        val activity = (requireActivity() as InsetterActivity)
        activity.addStatusBarPadding(header)

        activity.addNavigationBarPadding(binding.layoutProfileBasicContainerHolder.layoutScrollViewContent)
        activity.addNavigationBarPadding(binding.layoutProfileAdvancedContainerHolder.layoutScrollViewContent)

        header.layoutParams.height = header.layoutParams.height + activity.statusBarInsetHeight.top
        header.requestLayout()
    }

    private fun setupProfile() {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.getAccountBalance().collect { nodeBalance ->
                if (nodeBalance == null) return@collect

                nodeBalance.balance.asFormattedString().let { balance ->
                    binding.layoutProfileBasicInfoHolder.profileTextViewSatBalance.text = balance
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.accountOwnerStateFlow.collect { contactOwner ->
                contactOwner?.let { owner ->
                    owner.photoUrl?.value?.let { url ->
                        imageLoader.load(
                            binding.layoutProfileBasicInfoHolder.profileImageViewPicture,
                            url,
                            ImageLoaderOptions.Builder()
                                .placeholderResId(R.drawable.ic_profile_avatar_circle)
                                .transformation(Transformation.CircleCrop)
                                .build()
                        )
                    } ?: binding.layoutProfileBasicInfoHolder.profileImageViewPicture
                        .setImageDrawable(
                            ContextCompat.getDrawable(
                                binding.root.context,
                                R.drawable.ic_profile_avatar_circle
                            )
                        )

                    binding.layoutProfileBasicInfoHolder.profileTextViewName.text =
                        owner.alias?.value ?: ""

                    binding.layoutProfileBasicContainerHolder.profileUserNameEditText.setText(owner.alias?.value ?: "")
                    binding.layoutProfileBasicContainerHolder.profileAddressEditText.setText(owner.nodePubKey?.value ?: "")
                    binding.layoutProfileBasicContainerHolder.profileRouteHintEditText.setText(owner.routeHint?.value ?: "")
                }
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: ProfileViewState) {
//        TODO("Not yet implemented")
    }
}