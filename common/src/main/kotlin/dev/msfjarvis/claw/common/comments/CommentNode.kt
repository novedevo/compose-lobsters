/*
 * Copyright © 2022-2023 Harsh Shandilya.
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package dev.msfjarvis.claw.common.comments

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.HorizontalDivider
import dev.msfjarvis.claw.database.local.PostComments
import dev.msfjarvis.claw.model.Comment

internal data class CommentNode(
  val comment: Comment,
  var parent: CommentNode? = null,
  val children: MutableList<CommentNode> = mutableListOf(),
  val isUnread: Boolean = false,
  var isExpanded: Boolean = true,
  var indentLevel: Int
) {
  fun addChild(child: CommentNode) {
    if (comment.shortId == child.comment.parentComment) {
      children.add(child)
      child.parent = this
    } else {
      child.indentLevel += 1
      children.lastOrNull()?.addChild(child)
    }
  }
}

internal fun createListNode(
  comments: List<Comment>,
  commentState: PostComments?
): MutableList<CommentNode> {
  val commentNodes = mutableListOf<CommentNode>()
  val isUnread = { id: String -> commentState?.commentIds?.contains(id) == false }

  for (i in comments.indices) {
    if (comments[i].parentComment == null) {
      commentNodes.add(
        CommentNode(
          comment = comments[i],
          isUnread = isUnread(comments[i].shortId),
          indentLevel = 1
        )
      )
    } else {
      commentNodes.lastOrNull()?.let {
        it.addChild(
          CommentNode(
            comment = comments[i],
            isUnread = isUnread(comments[i].shortId),
            indentLevel = it.indentLevel + 1
          )
        )
      }
    }
  }

  return commentNodes
}

internal fun setExpanded(commentNode: CommentNode, expanded: Boolean): CommentNode {
  commentNode.isExpanded = expanded

  if (commentNode.children.isNotEmpty()) {
    commentNode.children.forEach { setExpanded(it, expanded) }
  }
  return commentNode
}

internal fun findTopMostParent(node: CommentNode): CommentNode {
  val parent = node.parent
  return if (parent != null) {
    findTopMostParent(parent)
  } else {
    node
  }
}

internal fun LazyListScope.nodes(
  nodes: List<CommentNode>,
  htmlConverter: HTMLConverter,
  toggleExpanded: (CommentNode) -> Unit,
) {
  nodes.forEach { node ->
    node(
      node = node,
      htmlConverter = htmlConverter,
      toggleExpanded = toggleExpanded,
    )
  }
}

private fun LazyListScope.node(
  node: CommentNode,
  htmlConverter: HTMLConverter,
  toggleExpanded: (CommentNode) -> Unit,
) {
  // Skip the node if neither the node nor its parent is expanded
  if (!node.isExpanded && node.parent?.isExpanded == false) {
    return
  }
  item {
    CommentEntry(
      commentNode = node,
      htmlConverter = htmlConverter,
      toggleExpanded = toggleExpanded,
    )
    HorizontalDivider()
  }
  if (node.children.isNotEmpty()) {
    nodes(
      node.children,
      htmlConverter = htmlConverter,
      toggleExpanded = toggleExpanded,
    )
  }
}
