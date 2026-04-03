/// A stub for dart:js to allow compilation on non-web platforms.
class JsContext {
  bool hasProperty(Object property) => false;
  dynamic operator [](Object property) => null;
}

final JsContext context = JsContext();
