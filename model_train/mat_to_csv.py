import csv
from numpy import squeeze
from scipy.io import loadmat

def convert_mat_to_csv(mat_name, csv_name):

    with open(csv_name, 'w', newline='') as csvfile:

        mat = loadmat(mat_name)

        print(mat)

        csvwriter = csv.writer(csvfile)
        csvwriter.writerow(['bbox_x1','bbox_y1','bbox_x2','bbox_y2', 'image_name'])

        for annotation in mat['annotations'][0]:
            x1 = squeeze(annotation['bbox_x1'])
            y1 = squeeze(annotation['bbox_y1'])
            x2 = squeeze(annotation['bbox_x2'])
            y2 = squeeze(annotation['bbox_y2'])
            # cls = squeeze(annotation['class'])
            image_name = str(squeeze(annotation['fname']))
            csvwriter.writerow([x1, y1, x2, y2, cls, image_name])

convert_mat_to_csv('devkit/cars_train_annos.mat', 'cars_train_annos.csv')
