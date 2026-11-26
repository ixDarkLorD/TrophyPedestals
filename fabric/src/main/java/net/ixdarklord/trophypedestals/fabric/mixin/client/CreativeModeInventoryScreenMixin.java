package net.ixdarklord.trophypedestals.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.ixdarklord.trophypedestals.pedestal.MobTrophySearch;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric has no search bar for mod tabs, so the mob trophies tab passes for the search tab where the screen shows, focuses
 * and types into the search box, and its results come from the tab's own trophies instead of the global search tree.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {
    @Shadow private static CreativeModeTab selectedTab;
    @Shadow private EditBox searchBox;
    @Shadow private float scrollOffs;

    @WrapOperation(method = {"charTyped", "preeditUpdated", "keyPressed", "selectTab", "refreshCurrentTabContents"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CreativeModeTab;getType()Lnet/minecraft/world/item/CreativeModeTab$Type;"))
    private CreativeModeTab.Type trophypedestals$searchType(CreativeModeTab tab, Operation<CreativeModeTab.Type> original) {
        return MobTrophySearch.searchable(tab) ? CreativeModeTab.Type.SEARCH : original.call(tab);
    }

    /** The box sits right of the title on this tab (its background draws a shorter box), where vanilla's is on the search tab. */
    @Inject(method = "selectTab", at = @At("TAIL"))
    private void trophypedestals$placeSearchBox(CreativeModeTab tab, CallbackInfo ci) {
        int width = MobTrophySearch.searchable(selectedTab) ? MobTrophySearch.SEARCH_WIDTH : 80;
        int x = MobTrophySearch.searchable(selectedTab) ? 171 - width : 82;
        this.searchBox.setWidth(width);
        this.searchBox.setX(((AbstractContainerScreenAccessor) this).trophypedestals$leftPos() + x);
    }

    @Inject(method = "refreshSearchResults", at = @At("HEAD"), cancellable = true)
    private void trophypedestals$searchTrophies(CallbackInfo ci) {
        if (!MobTrophySearch.searchable(selectedTab)) return;
        CreativeModeInventoryScreen.ItemPickerMenu menu = ((CreativeModeInventoryScreen) (Object) this).getMenu();
        menu.items.clear();
        menu.items.addAll(MobTrophySearch.filter(selectedTab.getDisplayItems(), this.searchBox.getValue()));
        this.scrollOffs = 0.0F;
        menu.scrollTo(0.0F);
        ci.cancel();
    }
}
