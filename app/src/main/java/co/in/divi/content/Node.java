package co.in.divi.content;

import java.util.ArrayList;

import org.json.JSONException;

import android.database.Cursor;
import android.util.Log;
import co.in.divi.util.LogConfig;

import com.google.gson.Gson;

public class Node {

	public static final int	NODE_TYPE_CHAPTER		= 0;
	public static final int	NODE_TYPE_TOPIC			= 1;
	public static final int	NODE_TYPE_PAGE			= 2;
	public static final int	NODE_TYPE_ASSESSMENT	= 3;

	// public static final int NODE_TYPE_RESOURCE = 4;

	public static String getNodeTypeName(int nodeType) {
		switch (nodeType) {
		case NODE_TYPE_CHAPTER:
			return "chapter";
		case NODE_TYPE_TOPIC:
			return "topic";
		case NODE_TYPE_PAGE:
			return "page";
		case NODE_TYPE_ASSESSMENT:
			return "exercise";
			// case NODE_TYPE_RESOURCE:
			// return "resource";
		}
		return null;
	}

	public String	id;
	public String	bookId;
	public String	parentId;
	public int		order;
	public int		type;
	public String	name;
	public String	parentName;		// helper to avoid extra db call for breadcrumb
	public String	content;
	public String	courseId;

	// for storing additional computed data.. like card for topic
	public Object	tag			= null;

	// computed:
	ArrayList<Node>	children	= null;
	Node			parent		= null; // check if we need to fill this?

	private Node() {
	}

	public static Node fromDBRow(Cursor cursor) throws JSONException {
		Node node = new Node();
		node.id = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_NODE_ID));
		node.bookId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_NODE_BOOK_ID));
		node.parentId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_NODE_PARENT_ID));
		node.order = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_NODE_ORDER));
		node.type = getNodeId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_NODE_TYPE)));
		node.name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_NODE_NAME));
		node.parentName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_NODE_PARENT_NAME));
		node.content = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_NODE_CONTENT));
		node.courseId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_NODE_COURSE_ID));
		// fill the tag (addl. metadata)
		switch (node.type) {
		case NODE_TYPE_TOPIC:
			node.tag = new Gson().fromJson(node.content, Topic.class);
			break;
		case NODE_TYPE_ASSESSMENT:
			node.tag = new Gson().fromJson(node.content, AssessmentFileModel.class);
			break;
		// case NODE_TYPE_RESOURCE:
		// node.tag = Resource.fromJSON(node.content);
		// break;
		default:
			break;
		}
		if (LogConfig.DEBUG_DBHELPER)
			Log.d("Node", "returning node=" + node.id);
		return node;
	}

	private static int getNodeId(String nodeIdString) {
		if ("chapter".equals(nodeIdString)) {
			return NODE_TYPE_CHAPTER;
		} else if ("topic".equals(nodeIdString)) {
			return NODE_TYPE_TOPIC;
		} else if ("page".equals(nodeIdString)) {
			return NODE_TYPE_PAGE;
		} else if ("exercise".equals(nodeIdString)) {
			return NODE_TYPE_ASSESSMENT;
			// } else if ("resource".equals(nodeIdString)) {
			// return NODE_TYPE_RESOURCE;
		}
		return -1;
	}

	public Object getTag() {
		return tag;
	}

	public ArrayList<Node> getChildren() {
		if (this.children == null)
			this.children = new ArrayList<Node>();
		return children;
	}

	public void addChild(Node child) {
		if (this.children == null)
			this.children = new ArrayList<Node>();
		this.children.add(child);
	}

	public Node getParent() {
		return parent;
	}

	public void setParent(Node parent) {
		this.parent = parent;
	}

}
