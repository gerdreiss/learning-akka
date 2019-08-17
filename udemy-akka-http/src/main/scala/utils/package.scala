package object utils {
  def using[A, T <: AutoCloseable](t: T)(f: T => A): A =
    try {
      f(t)
    } finally {
      if (t != null) t.close()
    }
}
