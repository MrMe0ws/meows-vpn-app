package app.meows.vpn.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.meows.vpn.R

/** Верх экрана — только название: все действия живут в нижней панели (зона большого пальца), см. [HomeToolbar]. */
@Composable
fun HomeTopBar() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.logo_icon), null, Modifier.size(28.dp))
        Spacer(Modifier.width(10.dp))
        Text("Мяус VPN", style = MaterialTheme.typography.titleLarge)
    }
}