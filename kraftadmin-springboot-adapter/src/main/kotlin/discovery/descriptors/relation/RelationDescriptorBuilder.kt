package discovery.descriptors.relation

import com.kraftadmin.enums.FormInputType
import com.kraftadmin.spi.AbstractResource
import javax.management.relation.RelationType

//class RelationDescriptorBuilder {
//
//    fun build(resource: AbstractResource<*>) {
//
////        resource.columns
////            .filter {
////                it.type == FormInputType.RELATION.name ||
////                        it.type == FormInputType.MULTI_RELATION.name
////            }
////            .forEach { column ->
////
////                resource.relations += RelationDescriptor(
////                    field = column.name,
////                    lookup = column.lookup!!
////                )
////
////            }
//
//
//    }
//
//}

object RelationDescriptorBuilder {
    // Moved helper logic here
//    fun getRelationType(javaField: Field, prop: KProperty<*>): RelationType? {
//        return when {
//            isRelationAnnotationPresent(javaField, prop, OneToOne::class) -> RelationType.ONE_TO_ONE
//            isRelationAnnotationPresent(javaField, prop, ManyToOne::class) -> RelationType.MANY_TO_ONE
//            isRelationAnnotationPresent(javaField, prop, ManyToMany::class) -> RelationType.MANY_TO_MANY
//            isRelationAnnotationPresent(javaField, prop, OneToMany::class) -> RelationType.ONE_TO_MANY
//            else -> null
//        }
//    }

//    fun resolveTargetEntity(javaField: Field, prop: KProperty<*>, type: RelationType?): KClass<*>? {
//        return when (type) {
//            RelationType.MANY_TO_ONE, RelationType.ONE_TO_ONE -> prop.returnType.classifier as? KClass<*>
//            RelationType.MANY_TO_MANY, RelationType.ONE_TO_MANY ->
//                prop.returnType.arguments.firstOrNull()?.type?.classifier as? KClass<*>
//                    ?: (javaField.genericType as? ParameterizedType)?.actualTypeArguments?.firstOrNull()?.let { (it as? Class<*>)?.kotlin }
//            else -> null
//        }
//    }

}