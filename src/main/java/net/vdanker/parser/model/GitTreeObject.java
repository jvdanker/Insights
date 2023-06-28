package net.vdanker.parser.model;

import org.eclipse.jgit.lib.ObjectId;

import java.io.InputStream;
import java.io.OutputStream;

public record GitTreeObject(ObjectId objectId, String name, InputStream is) {
}
