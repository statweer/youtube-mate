-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
-dontwarn javax.naming.InvalidNameException
-dontwarn javax.naming.NamingException
-dontwarn javax.naming.directory.Attribute
-dontwarn javax.naming.directory.Attributes
-dontwarn javax.naming.ldap.LdapName
-dontwarn javax.naming.ldap.Rdn
-dontwarn org.ietf.jgss.GSSContext
-dontwarn org.ietf.jgss.GSSCredential
-dontwarn org.ietf.jgss.GSSException
-dontwarn org.ietf.jgss.GSSManager
-dontwarn org.ietf.jgss.GSSName
-dontwarn org.ietf.jgss.Oid
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.impl.StaticMDCBinder

# Keep Activities
-keep public class * extends androidx.activity.ComponentActivity

# Keep Application class
-keep public class *.YouTubeMateApplication extends android.app.Application

# Keep Kotlin Main Function
#-keep class me.moallemi.youtubemate.MainKt {
#    public static void main(java.lang.String[]);
#}

# Keep Compose UI
-keep class androidx.compose.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.animation.** { *; }

# Keep Compose Preview
-keep class androidx.compose.ui.tooling.** { *; }

# Keep DataStore Preferences
-keep class androidx.datastore.** { *; }

# Keep Kotlin Serialization
-keep class kotlinx.serialization.** { *; }
-keep class kotlinx.serialization.json.** { *; }

# Keep Coil
-keep class coil.** { *; }
-keep class coil.compose.** { *; }
-keep class coil.network.** { *; }

# Keep Ktor
-keep class io.ktor.** { *; }

# Keep Google API Client
-keep class com.google.api.** { *; }
-keep class com.google.api.services.youtube.** { *; }

# Keep ViewModels
-keep class androidx.lifecycle.ViewModel
-keep class androidx.lifecycle.viewmodel.** { *; }

# Keep Coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep OkHttp
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Keep Resources
-keep class androidx.compose.ui.res.** { *; }

# Keep Annotations
-keep @androidx.annotation.Keep class *
#-keep @androidx.annotation.KeepWithMembers class *
#-keep @androidx.annotation.KeepSubclasses class *