package info.a24731.syllabol.domain

import java.util.UUID

opaque type JobId = UUID
object JobId:
  def generate(): JobId        = UUID.randomUUID()
  def apply(uuid: UUID): JobId = uuid
