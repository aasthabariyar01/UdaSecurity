module securityService {
    requires  imageService;
    requires java.desktop;
    requires com.miglayout.swing;
    requires com.google.gson;
    requires java.prefs;
    requires com.google.common;
    opens com.udacity.catpoint.SecurityService.data to com.google.gson;

}


