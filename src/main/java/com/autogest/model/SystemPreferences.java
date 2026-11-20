package com.autogest.model;

/**
 * Preferencias generales del sistema (mockup Preferencias del sistema V1):
 * región y formato, interfaz, comportamiento, tablas y sesión. Los valores
 * por defecto están en el constructor y en {@link #defaults()}.
 *
 * <p>Principles: Information Expert, High Cohesion.</p>
 */
public class SystemPreferences {

    private String language;
    private String country;
    private String timeZone;
    private String currency;
    private String dateFormat;
    private String timeFormat;
    private String decimalSeparator;
    private String sidebarMode;
    private int pageSize;
    private String defaultSort;
    private boolean confirmBeforeDelete;
    private boolean showSaveConfirmation;
    private boolean openLastModule;
    private boolean showContextHelp;
    private boolean saveRecentFilters;
    private boolean showExtraTableInfo;
    private boolean autoLogout;
    private int inactivityMinutes;

    /** Crea las preferencias con los valores por defecto del taller (Colombia). */
    public SystemPreferences() {
        this.language = "Español";
        this.country = "Colombia";
        this.timeZone = "(GMT-05:00) Bogotá, Lima, Quito";
        this.currency = "Peso colombiano (COP)";
        this.dateFormat = "DD/MM/AAAA";
        this.timeFormat = "24 horas";
        this.decimalSeparator = "Coma (,)";
        this.sidebarMode = "Expandido";
        this.pageSize = 10;
        this.defaultSort = "Más recientes primero";
        this.confirmBeforeDelete = true;
        this.showSaveConfirmation = true;
        this.openLastModule = false;
        this.showContextHelp = true;
        this.saveRecentFilters = true;
        this.showExtraTableInfo = true;
        this.autoLogout = true;
        this.inactivityMinutes = 30;
    }

    /**
     * Preferencias de fábrica (para «Restaurar valores predeterminados»).
     *
     * @return una instancia nueva con los valores por defecto
     */
    public static SystemPreferences defaults() {
        return new SystemPreferences();
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getTimeFormat() {
        return timeFormat;
    }

    public void setTimeFormat(String timeFormat) {
        this.timeFormat = timeFormat;
    }

    public String getDecimalSeparator() {
        return decimalSeparator;
    }

    public void setDecimalSeparator(String decimalSeparator) {
        this.decimalSeparator = decimalSeparator;
    }

    public String getSidebarMode() {
        return sidebarMode;
    }

    public void setSidebarMode(String sidebarMode) {
        this.sidebarMode = sidebarMode;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public String getDefaultSort() {
        return defaultSort;
    }

    public void setDefaultSort(String defaultSort) {
        this.defaultSort = defaultSort;
    }

    public boolean isConfirmBeforeDelete() {
        return confirmBeforeDelete;
    }

    public void setConfirmBeforeDelete(boolean confirmBeforeDelete) {
        this.confirmBeforeDelete = confirmBeforeDelete;
    }

    public boolean isShowSaveConfirmation() {
        return showSaveConfirmation;
    }

    public void setShowSaveConfirmation(boolean showSaveConfirmation) {
        this.showSaveConfirmation = showSaveConfirmation;
    }

    public boolean isOpenLastModule() {
        return openLastModule;
    }

    public void setOpenLastModule(boolean openLastModule) {
        this.openLastModule = openLastModule;
    }

    public boolean isShowContextHelp() {
        return showContextHelp;
    }

    public void setShowContextHelp(boolean showContextHelp) {
        this.showContextHelp = showContextHelp;
    }

    public boolean isSaveRecentFilters() {
        return saveRecentFilters;
    }

    public void setSaveRecentFilters(boolean saveRecentFilters) {
        this.saveRecentFilters = saveRecentFilters;
    }

    public boolean isShowExtraTableInfo() {
        return showExtraTableInfo;
    }

    public void setShowExtraTableInfo(boolean showExtraTableInfo) {
        this.showExtraTableInfo = showExtraTableInfo;
    }

    public boolean isAutoLogout() {
        return autoLogout;
    }

    public void setAutoLogout(boolean autoLogout) {
        this.autoLogout = autoLogout;
    }

    public int getInactivityMinutes() {
        return inactivityMinutes;
    }

    public void setInactivityMinutes(int inactivityMinutes) {
        this.inactivityMinutes = inactivityMinutes;
    }
}
