package d4s.models.table.index

final case class IndexToUpdate(create: Set[ProvisionedGlobalIndex[Nothing, Nothing]], modify: Set[GlobalIndexUpdate], delete: Set[String])
