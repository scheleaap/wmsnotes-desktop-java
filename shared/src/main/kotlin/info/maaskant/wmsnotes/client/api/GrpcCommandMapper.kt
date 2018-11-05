package info.maaskant.wmsnotes.client.api

import com.google.protobuf.ByteString
import info.maaskant.wmsnotes.model.AddAttachmentCommand
import info.maaskant.wmsnotes.model.CreateNoteCommand
import info.maaskant.wmsnotes.model.DeleteAttachmentCommand
import info.maaskant.wmsnotes.model.DeleteNoteCommand
import info.maaskant.wmsnotes.server.command.grpc.Command
import javax.inject.Inject

class GrpcCommandMapper @Inject constructor() {
    fun toGrpcPostCommandRequest(command: info.maaskant.wmsnotes.model.Command): Command.PostCommandRequest {
        val builder = Command.PostCommandRequest.newBuilder()
        @Suppress("UNUSED_VARIABLE")
        val a: Any = when (command) { // Assign to variable to force a compilation error if 'when' expression is not exhaustive.
            is CreateNoteCommand -> builder.apply {
                noteId = command.noteId
                createNote = Command.PostCommandRequest.CreateNoteCommand.newBuilder().apply {
                    title = command.title
                }.build()
            }
            is DeleteNoteCommand -> builder.apply {
                noteId = command.noteId
                lastRevision = command.lastRevision
                deleteNote = Command.PostCommandRequest.DeleteNoteCommand.newBuilder().build()
            }
            is AddAttachmentCommand -> builder.apply {
                noteId = command.noteId
                lastRevision = command.lastRevision
                addAttachment = Command.PostCommandRequest.AddAttachmentCommand.newBuilder().apply {
                    name = command.name
                    content = ByteString.copyFrom(command.content)
                }.build()
            }
            is DeleteAttachmentCommand -> builder.apply {
                noteId = command.noteId
                lastRevision = command.lastRevision
                deleteAttachment = Command.PostCommandRequest.DeleteAttachmentCommand.newBuilder().apply {
                    name = command.name
                }.build()
            }
        }
        return builder.build()
    }
}