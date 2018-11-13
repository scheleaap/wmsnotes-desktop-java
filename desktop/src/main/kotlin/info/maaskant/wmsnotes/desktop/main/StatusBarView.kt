package info.maaskant.wmsnotes.desktop.main

import com.github.thomasnield.rxkotlinfx.observeOnFx
import info.maaskant.wmsnotes.client.synchronization.Synchronizer
import javafx.scene.layout.Priority
import tornadofx.*
import tornadofx.controlsfx.statusbar

class StatusBarView : View() {

    private val applicationModel: ApplicationModel by di()
    private val synchronizer: Synchronizer by di()

    override val root = statusbar {
        hbox {
            label {
                synchronizer.getConflicts()
                        .observeOnFx()
                        .subscribe {
                            text = "${it.size} conflicts"
                        }
            }
            region {
                hgrow = Priority.ALWAYS
            }
            progressindicator {
                progress = -1.0
                isVisible = false
                setPrefSize(16.0, 16.0)
                applicationModel.isSwitchingToNewSelection
                        .subscribe(this::setVisible)
            }
        }
    }

}
