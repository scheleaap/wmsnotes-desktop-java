package info.maaskant.wmsnotes.desktop.view

import info.maaskant.wmsnotes.desktop.app.Injector
import info.maaskant.wmsnotes.desktop.app.logger
import info.maaskant.wmsnotes.model.Model
import info.maaskant.wmsnotes.model.NoteCreatedEvent
import info.maaskant.wmsnotes.model.NoteDeletedEvent
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import javafx.scene.control.TreeItem
import tornadofx.*

data class NotebookNode(val id: String, val title: String)

class NodeView : View() {

    private val logger by logger()

    private val model: Model = Injector.instance.model()

    private val rootNode = TreeItem(NotebookNode(id = "root", title = "Root"))

    private val treeItemReferences = mutableMapOf<String, TreeItem<NotebookNode>>()

    override val root = treeview<NotebookNode> {
        root = rootNode
        root.isExpanded = true
        showRootProperty().set(false)
        cellFormat { text = it.title }
        onUserSelect {
            logger.debug("Selected: $it")
        }
    }

    init {
        model.events
                .observeOn(JavaFxScheduler.platform())
                .subscribe({
                    when (it) {
                        is NoteCreatedEvent -> noteCreated(it)
                        is NoteDeletedEvent -> noteDeleted(it)
                    }
                }, { logger.warn("Error", it) })
    }

    private fun noteCreated(e: NoteCreatedEvent) {
        logger.debug("Adding note ${e.noteId}")
        val node = NotebookNode(id = e.noteId, title = e.title)
        val treeItem = TreeItem(node)
        treeItemReferences.put(node.id, treeItem)
        rootNode += treeItem
    }

    private fun noteDeleted(e: NoteDeletedEvent) {
        logger.debug("Removing note ${e.noteId}")
        val treeItem = treeItemReferences.remove(e.noteId)
        rootNode.children.remove(treeItem)
    }
}
