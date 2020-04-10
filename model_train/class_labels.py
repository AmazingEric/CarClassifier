from numpy import squeeze
from scipy.io import loadmat

mat_file = 'devkit/cars_meta.mat'

def attach_labels_to_class(cars_mat, save_name):

    mat = loadmat(cars_mat)

    with open(save_name, 'w', newline='') as savefile:

        i = 1

        for class_name in mat['class_names'][0]:
            item = "%s\n" % class_name[0]
            savefile.write(item)
            i += 1

attach_labels_to_class(mat_file, 'class_labels.txt')
