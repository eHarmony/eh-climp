package com.eharmony.climp.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;


/**
 * 
 * @see http://stackoverflow.com/questions/4332264/wrapping-a-bytebuffer-with-an-inputstream
 */
public class ByteBufferBackedInputStream extends InputStream
{
  private ByteBuffer backendBuffer;

  public ByteBufferBackedInputStream(ByteBuffer backendBuffer) {
      this.backendBuffer = backendBuffer;
      
  }
  public void close() throws IOException {
      this.backendBuffer = null;
  }

  private void ensureStreamAvailable() throws IOException {
      if (this.backendBuffer == null) {
          throw new IOException("read on a closed InputStream!");
      }
  }

  @Override
  public int read() throws IOException {
      this.ensureStreamAvailable();
      return this.backendBuffer.hasRemaining() ? this.backendBuffer.get() & 0xFF : -1;
  }

  @Override
  public int read(byte[] buffer) throws IOException {
      return this.read(buffer, 0, buffer.length);
  }

  @Override
  public int read(byte[] buffer, int offset, int length) throws IOException {
      this.ensureStreamAvailable();
      if (offset >= 0 && length >= 0 && length <= buffer.length - offset) {
          if (length == 0) {
              return 0;
          }
          else {
              int remainingSize = Math.min(this.backendBuffer.remaining(), length);
              if (remainingSize == 0) {
                  return -1;
              }
              else {
                  this.backendBuffer.get(buffer, offset, remainingSize);
                  return remainingSize;
              }
          }
      }
      else {
          throw new IndexOutOfBoundsException();
      }
  }

  public long skip(long n) throws IOException {
      this.ensureStreamAvailable();
      if (n <= 0L) {
          return 0L;
      }
      int length = (int) n;
      int remainingSize = Math.min(this.backendBuffer.remaining(), length);
      this.backendBuffer.position(this.backendBuffer.position() + remainingSize);
      return (long) length;
  }

  public int available() throws IOException {
      this.ensureStreamAvailable();
      return this.backendBuffer.remaining();
  }

  public synchronized void mark(int var1) {
  }

  public synchronized void reset() throws IOException {
      throw new IOException("mark/reset not supported");
  }

  public boolean markSupported() {
      return false;
  }
}