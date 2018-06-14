package net.scalax.asuna.core.helper

abstract trait DataConfirm
trait DefinedData extends DataConfirm

trait WrapDataConfirm[R] {
  type DCType <: DataConfirm
}

object WrapDataConfirm {
  type Aux[E <: DataConfirm, R] = WrapDataConfirm[R] { type DCType = E }
  type Defined[R] = Aux[DefinedData, R]
}

trait TypeConfirm[R] {
  type DCType <: DataConfirm

}

object TypeConfirm {
  type Aux[E <: DataConfirm, R] = TypeConfirm[R] { type DCType = E }
  type Defined[R] = Aux[DefinedData, R]
}