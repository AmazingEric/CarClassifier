import os
import sys
import numpy as np
from skimage.io import imread
import matplotlib.pyplot as plt
import numpy as np
import keras
import pandas as pd
from PIL import Image
import cv2 as cv

from keras.applications.resnet50 import preprocess_input
from keras.preprocessing.image import ImageDataGenerator

from keras.applications.imagenet_utils import decode_predictions

from efficientnet.keras import EfficientNetB0,EfficientNetB7
from efficientnet.keras import center_crop_and_resize, preprocess_input
from keras.optimizers import SGD, Adam

# from keras.utils import plot_model
# from keras.models import Model
# from keras.layers import Input
# from keras.layers import Dense
# from keras.layers import Flatten
# from keras.layers import Activation
# from keras.layers import Dropout
# from keras.layers import Maximum
# from keras.layers import ZeroPadding2D
# from keras.layers.convolutional import Conv2D
from keras.layers.pooling import MaxPooling2D,AveragePooling2D
# from keras.layers.merge import concatenate
# from keras import regularizers
# from keras.layers import BatchNormalization
# from keras.optimizers import Adam, SGD
# from keras.preprocessing.image import ImageDataGenerator
# from keras.callbacks import ModelCheckpoint, ReduceLROnPlateau
# from keras.layers.advanced_activations import LeakyReLU
# from keras.utils import to_categorical

# from sklearn.model_selection import StratifiedKFold
# from skimage.transform import resize as imresize
# from tqdm import tqdm


def plot_loss_acc(history):
    plt.figure(figsize=(20,7))
    plt.subplot(1,2,1)
    plt.plot(history.history['loss'][1:])    
    plt.plot(history.history['val_loss'][1:])    
    plt.title('model loss')    
    plt.ylabel('val_loss')    
    plt.xlabel('epoch')    
    plt.legend(['Train','Validation'], loc='upper left')
    
    plt.subplot(1,2,2)
    plt.plot(history.history['acc'][1:])
    plt.plot(history.history['val_acc'][1:])
    plt.title('Model Accuracy')
    plt.ylabel('val_acc')
    plt.xlabel('epoch')
    plt.legend(['Train','Validation'], loc='upper left')
    plt.show()

class SnapshotCallbackBuilder:
    def __init__(self, nb_epochs, nb_snapshots, init_lr=0.1):
        self.T = nb_epochs
        self.M = nb_snapshots
        self.alpha_zero = init_lr

    def get_callbacks(self, model_prefix='Model'):

        callback_list = [
#             callbacks.ModelCheckpoint("./keras.model",monitor='val_loss', 
#                                    mode = 'min', save_best_only=True, verbose=1),
            swa,
            callbacks.LearningRateScheduler(schedule=self._cosine_anneal_schedule)
        ]

        return callback_list

    def _cosine_anneal_schedule(self, t):
        cos_inner = np.pi * (t % (self.T // self.M))  # t - 1 is used when t has 1-based indexing.
        cos_inner /= self.T // self.M
        cos_out = np.cos(cos_inner) + 1
        return float(self.alpha_zero / 2 * cos_out)

import keras.callbacks as callbacks

class SWA(keras.callbacks.Callback):
    
    def __init__(self, filepath, swa_epoch):
        super(SWA, self).__init__()
        self.filepath = filepath
        self.swa_epoch = swa_epoch 
    
    def on_train_begin(self, logs=None):
        self.nb_epoch = self.params['epochs']
        print('Stochastic weight averaging selected for last {} epochs.'
              .format(self.nb_epoch - self.swa_epoch))

    def on_epoch_end(self, epoch, logs=None):
        print('start')
        if epoch == self.swa_epoch:
            self.swa_weights = self.model.get_weights()
        elif epoch > self.swa_epoch:    
            for i in range(len(self.swa_weights)):
                self.swa_weights[i] = (self.swa_weights[i] * 
                    (epoch - self.swa_epoch) + self.model.get_weights()[i])/((epoch - self.swa_epoch)  + 1)

        else:
            pass

    def on_train_end(self, logs=None):
        self.model.set_weights(self.swa_weights)
        print('Final model parameters set to stochastic weight average.')
        self.model.save_weights(self.filepath)
        print('Final stochastic averaged weights saved to file.')


# In[12]:


train_datagen = ImageDataGenerator(
        rescale=1./255,
        shear_range=0.2,
        rotation_range=20.,
        width_shift_range=0.1,
        height_shift_range=0.1,
        zoom_range=[0.9, 1.25],
        brightness_range=[0.5, 1.5],
        horizontal_flip=True)

test_datagen = ImageDataGenerator(rescale=1./255)

train_generator = train_datagen.flow_from_directory(
        'data/train',
        target_size=(224, 224),
        batch_size=32,
        class_mode='categorical')

validation_generator = test_datagen.flow_from_directory(
        'data/valid',
        target_size=(224, 224),
        batch_size=32,
        class_mode='categorical')


# In[13]:


from keras.layers import Dense, Activation, Flatten, Dropout
from keras.models import Sequential, Model

def build_finetune_model(base_model, dropout, num_classes):

    x = base_model.output
    
    x = AveragePooling2D((5, 5), name='avg_pool')(x)
    x = Flatten()(x)
#     x = Dropout(0.25)(x)
#     x = Dense(512,activation='relu')(x)
    x = Dropout(dropout)(x)
    predictions = Dense(num_classes, activation='softmax', name='finalfc')(x)
    
    finetune_model = Model(inputs=base_model.input, outputs=predictions)

    return finetune_model


# In[14]:


HEIGHT = 224
WIDTH = 224

input_shape=(HEIGHT, WIDTH, 3)

FC_LAYERS = [1024]
dropout = 0.7
epochs = 30
swa = SWA('./keras_swa.model',epochs-3)

base_model = EfficientNetB0(weights='imagenet',
                            include_top=False,
                            input_shape=(HEIGHT, WIDTH, 3))

finetune_model = build_finetune_model(base_model, 
                                      dropout=dropout, 
                                      num_classes=196)

finetune_model.compile(optimizer='adam', loss='categorical_crossentropy', metrics=['accuracy'])

snapshot = SnapshotCallbackBuilder(nb_epochs=epochs,nb_snapshots=1,init_lr=1e-3)

history = finetune_model.fit_generator(generator=train_generator,
                                        validation_data=validation_generator,
                                        steps_per_epoch=3,
                                        epochs=epochs,verbose=2,validation_steps=10,callbacks=snapshot.get_callbacks())


try:
    finetune_model.load_weights('./keras_swa.model')
except Exception as e:
    print(e)


# In[15]:


plot_loss_acc(history)


# In[16]:


from tqdm import tqdm_notebook
import scipy.io as sio

num_samples,all_preds = 8041,[]
out = open('result.txt', 'a')
for i in tqdm_notebook(range(num_samples)):
    filename = os.path.join('data/test', '%05d.jpg' % (i + 1))
    bgr_img = cv.imread(filename)
    rgb_img = cv.resize(cv.cvtColor(bgr_img, cv.COLOR_BGR2RGB)/255,(224,224))
    rgb_img = np.expand_dims(rgb_img, 0)
    preds = finetune_model.predict(rgb_img)
    class_id = np.argmax(preds)
    all_preds.append(class_id)
    out.write('{}\n'.format(str(class_id + 1)))
    
out.close()


# In[17]:


labels = sio.loadmat('cars_test_annos_withlabels.mat')
actual_preds = np.array(labels['annotations']['class'],dtype=np.int)-1;
actual_preds = actual_preds.squeeze()
all_preds = np.array(all_preds)


# In[18]:


print('accuracy = ',(all_preds==actual_preds).sum()/len(actual_preds)) 
