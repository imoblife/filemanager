package com.filemanager.occupancy;


import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by wuhao on 2015/6/15.
 */
public class FileTreeNode<T> implements Iterable<T> {

    public static final int DEFAULT_FILE_BLOCK_SIZE = 4096;

    //data use for file
    public File data;
    //the file size
    public long size = 0;

    FileTreeNode<T> parent = null;
    ArrayList<FileTreeNode<T>> children;

    public FileTreeNode(File data) {
        this.data = data;
        this.children = new ArrayList<FileTreeNode<T>>();
        initSize();
    }

    private void initSize() {
        try {
            if (data.isFile() && data.canRead()) {
                size = data.length();
            } else if (data.isDirectory()) {
                size = DEFAULT_FILE_BLOCK_SIZE;
            }
        } catch (Exception e) {
            size = 0;
        }
    }

    public FileTreeNode<T> addChild(File child) {
        FileTreeNode<T> childNode = new FileTreeNode<T>(child);
        childNode.parent = this;
        size = size + childNode.size;
        this.children.add(childNode);
        return childNode;
    }

    @Override
    public Iterator<T> iterator() {
        //add something
        return null;
    }

    public final void refresh() {
        initSize();
        Iterator iterator = children.iterator();
        while (iterator.hasNext()) {
            FileTreeNode<T> tmp = (FileTreeNode<T>) iterator.next();
            size += tmp.size;
        }
    }
}

