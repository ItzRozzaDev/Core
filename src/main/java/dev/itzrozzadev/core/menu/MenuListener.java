package dev.itzrozzadev.core.menu;

import dev.itzrozzadev.core.event.EventRegistry;
import dev.itzrozzadev.core.menu.button.Button;
import dev.itzrozzadev.core.messages.Messenger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

public class MenuListener implements Listener {


	public MenuListener() {
		try {
			Class.forName("org.bukkit.event.player.PlayerSwapHandItemsEvent");

			EventRegistry.registerEvent(new OffHandListener());
		} catch (final Throwable ignored) {

		}

		try {
			Class.forName("org.bukkit.event.inventory.InventoryDragEvent");
			EventRegistry.registerEvent(new DragListener());
		} catch (final Throwable ignored) {

		}
	}


	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onMenuClose(final InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player))
			return;

		final Player player = (Player) event.getPlayer();
		final Menu menu = Menu.getCurrentMenu(player);

		if (menu != null)
			menu.handleClose(event.getInventory());

	}

	/**
	 * Handles clicking in menus
	 *
	 * @param event the event
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void onMenuClick(final InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player))
			return;

		final Player player = (Player) event.getWhoClicked();
		final Menu menu = Menu.getCurrentMenu(player);
		if (menu != null) {
			final int slot = event.getSlot();
			final ItemStack slotItem = event.getCurrentItem();
			final ItemStack cursor = event.getCursor();
			final InventoryAction action = event.getAction();
			final InventoryView view = event.getView();
			final Inventory clickedInventory = slot < 0 ? null : view.getTopInventory() != null && slot < view.getTopInventory().getSize() ? view.getTopInventory() : view.getBottomInventory();
			final MenuClickLocation whereClicked = clickedInventory != null
					? clickedInventory.getType() == InventoryType.CHEST
					? MenuClickLocation.MENU : MenuClickLocation.PLAYER_INVENTORY : MenuClickLocation.OUTSIDE;
			final ClickType clickType = event.getClick();
			final boolean allowed = menu.isActionAllowed(whereClicked, slot, slotItem, cursor, action);
			if (action.toString().contains("PICKUP") || action.toString().contains("PLACE") || action.toString().equals("SWAP_WITH_CURSOR") || action == InventoryAction.CLONE_STACK || clickType.isShiftClick() || clickType == ClickType.MIDDLE || clickType == ClickType.DROP) {
				try {
					final Button button = menu.getButton(slot);
					final MenuClick click = new MenuClick(player, menu, slot, clickType);

					if (button != null)
						menu.onButtonClick(button, click);
					else
						menu.onMenuClick(click);

				} catch (final Throwable t) {
					player.closeInventory();
					t.printStackTrace();
					Messenger.sendError(player, "An error occurred while handling the click");

				}
			}
			if (!allowed) {
				event.setCancelled(true);
				player.updateInventory();
			}
		}
	}

	private static final class OffHandListener implements Listener {

		@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
		public void onSwapItems(final PlayerSwapHandItemsEvent event) {
			if (Menu.getCurrentMenu(event.getPlayer()) != null) {
				event.setCancelled(true);
				event.getPlayer().updateInventory();
			}
		}
	}

	private static final class DragListener implements Listener {
		@EventHandler
		public void onInventoryDragTop(final InventoryDragEvent event) {
			if (!(event.getWhoClicked() instanceof Player))
				return;

			final Player player = (Player) event.getWhoClicked();
			final Menu menu = Menu.getCurrentMenu(player);

			if (menu != null && event.getView().getType() == InventoryType.CHEST) {
				final int size = event.getView().getTopInventory().getSize();

				for (final int slot : event.getRawSlots()) {
					if (slot > size)
						continue;

					final ItemStack cursor = event.getCursor() == null ? event.getOldCursor() : event.getCursor();

					if (!menu.isActionAllowed(MenuClickLocation.MENU, slot, event.getNewItems().get(slot), cursor, InventoryAction.PLACE_SOME)) {
						event.setCancelled(true);

						return;
					}
				}
			}
		}
	}

}