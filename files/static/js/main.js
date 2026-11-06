import {initializeUi, resetFilters, setQuickFilter} from './ui.js';
import {initializeCharts} from './charts.js';
import {initializeIgdbSearch} from './igdb-search.js';
import {deleteGame, initializeGameModal, openGameDetails, resetForm, saveGame} from './games.js';
import {deleteOwnAccount, initializeSettingsTabs, initializeThemePicker, openAccountModal, saveAccount} from './account.js';
import {openAdminModal} from './admin.js';
import {initializePwa} from './pwa.js';

Object.assign(window, {
    resetFilters, setQuickFilter, resetForm, openGameDetails, saveGame, deleteGame,
    openAccountModal, saveAccount, deleteOwnAccount, openAdminModal
});

initializeUi();
initializeCharts();
initializeIgdbSearch();
initializeGameModal();
initializeThemePicker();
initializeSettingsTabs();
initializePwa();

import {initializeDiscovery} from './discover.js';
initializeDiscovery();

import {initializeFriends} from './friends.js';
initializeFriends();

import {initializeGameDefaults} from './game-defaults.js';
initializeGameDefaults();
