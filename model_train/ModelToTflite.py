import tensorflow as tf
from tensorflow.python.keras.layers import Dense, Activation, Flatten, Dropout
from tensorflow.python.keras.models import Sequential, Model, load_model
from tensorflow.python.keras.layers.pooling import MaxPooling2D,AveragePooling2D
from efficientnet.tfkeras import EfficientNetB0,EfficientNetB7

'''
def build_finetune_model(base_model, dropout, num_classes):

    x = base_model.output
    
    x = AveragePooling2D((5, 5), name='avg_pool')(x)
    x = Flatten()(x)
    x = Dropout(0.25)(x)
    x = Dense(512,activation='relu')(x)
    x = Dropout(dropout)(x)
    predictions = Dense(num_classes, activation='softmax', name='finalfc')(x)
    
    finetune_model = Model(inputs=base_model.input, outputs=predictions)

    return finetune_model

base_model = EfficientNetB0(weights='imagenet',
                            include_top=False,
                            input_shape=(224, 224, 3))

finetune_model = build_finetune_model(base_model, 
                                      dropout=0.7, 
                                      num_classes=196)

finetune_model.compile(optimizer='adam', loss='categorical_crossentropy', metrics=['accuracy'])

finetune_model.load_weights('./keras_swa.model')
'''

model = load_model('./keras_swa.h5')
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model  = converter.convert()
open('newModel.tflite', 'wb').write(tflite_model)