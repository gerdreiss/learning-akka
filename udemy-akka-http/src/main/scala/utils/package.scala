package object utils {
  def using[T <: AutoCloseable, A](t: => T)(f: T => A): A =
    try {
      f(t)
    } finally {
      if (t != null) t.close()
    }
}
