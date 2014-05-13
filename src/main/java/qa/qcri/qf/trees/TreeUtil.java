package qa.qcri.qf.trees;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import qa.qcri.qf.trees.nodes.RichNode;

import com.google.common.base.Function;

public class TreeUtil {

	/**
	 * Traverses the tree in a recursive preorder fashion
	 * 
	 * @param tree
	 *            the tree to traverse
	 * @return a list of nodes
	 */
	public static List<RichNode> getNodes(RichNode tree) {
		List<RichNode> nodes = new ArrayList<>();

		nodes.add(tree);

		List<RichNode> children = tree.getChildren();
		if (children.isEmpty()) {
			return nodes;
		} else {
			for (RichNode child : children) {
				nodes.addAll(getNodes(child));
			}
		}

		return nodes;
	}

	/**
	 * Traverses the tree in a recursive preorder fashion A node is returned if
	 * it satisfies the filteringCriteria which performs a test on a RichNode
	 * and returns a boolean value
	 * 
	 * @param tree
	 *            the tree to traverse
	 * @param filteringCriteria
	 *            the test performed on each node
	 * @return a list of tree nodes satisfying the filtering criteria
	 */
	public static List<RichNode> getNodesWithFilter(RichNode tree,
			Function<RichNode, Boolean> filteringCriteria) {
		List<RichNode> nodes = new ArrayList<>();

		if (filteringCriteria.apply(tree)) {
			nodes.add(tree);
		}

		List<RichNode> children = tree.getChildren();
		if (children.isEmpty()) {
			return nodes;
		} else {
			for (RichNode child : children) {
				nodes.addAll(getNodesWithFilter(child, filteringCriteria));
			}
		}

		return nodes;
	}

	/**
	 * Returns the leaves of a tree
	 * 
	 * @param tree
	 *            the tree to traverse
	 * @return the leaves of the tree
	 */
	public static List<RichNode> getLeaves(RichNode tree) {
		return TreeUtil.getNodesWithFilter(tree,
				new Function<RichNode, Boolean>() {
					@Override
					public Boolean apply(RichNode node) {
						return node.isLeaf();
					}
				});
	}

	/**
	 * Returns the nodes of the tree which are not leaves
	 * 
	 * @param tree
	 *            the tree to traverse
	 * @return the nodes of the tree which are not leaves
	 */
	public static List<RichNode> getNonLeaves(RichNode tree) {
		return TreeUtil.getNodesWithFilter(tree,
				new Function<RichNode, Boolean>() {
					@Override
					public Boolean apply(RichNode node) {
						return !node.isLeaf();
					}
				});
	}

	/**
	 * Returns the parents of the leaves
	 * 
	 * @param tree
	 *            the tree to traverse
	 * @return the parent nodes of the leaves
	 */
	public static List<RichNode> getLeavesParents(RichNode tree) {
		List<RichNode> leavesParents = new ArrayList<>();

		List<RichNode> leaves = TreeUtil.getLeaves(tree);
		Set<RichNode> parents = new HashSet<>();

		for (RichNode leaf : leaves) {
			RichNode parent = leaf.getParent();
			if (!parents.contains(parent)) {
				leavesParents.add(parent);
				parents.add(parent);
			}
		}

		return leavesParents;
	}
	
	/**
	 * Returns the grandparents of the leaves
	 * 
	 * @param tree
	 *            the tree to traverse
	 * @return the grandparent nodes of the leaves
	 */
	public static List<RichNode> getLeavesGrandParents(RichNode tree) {
		List<RichNode> leavesGrandParents = new ArrayList<>();

		List<RichNode> leaves = TreeUtil.getLeaves(tree);
		Set<RichNode> grandParents = new HashSet<>();

		for (RichNode leaf : leaves) {
			RichNode parent = leaf.getParent();
			if(parent != null) {
				RichNode grandParent = parent.getParent();
				if (grandParent != null && !grandParents.contains(parent)) {
					leavesGrandParents.add(parent);
					grandParents.add(parent);
				}
			}
		}

		return leavesGrandParents;
	}

}
